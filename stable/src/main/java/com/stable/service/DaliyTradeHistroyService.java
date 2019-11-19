package com.stable.service;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.stable.constant.Constant;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.EsDaliyBasicInfoDao;
import com.stable.es.dao.EsTradeHistInfoDaliyDao;
import com.stable.spider.sina.SinaSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;
/**
 * 日交易历史
 * @author roy
 *
 */
@Service
@Log4j2
public class DaliyTradeHistroyService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private SinaSpider sinaSpider;
	@Autowired
	private EsTradeHistInfoDaliyDao tradeHistDaliy;
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;

	// 全量获取历史记录（定时任务）-根据缓存是否需要重新获取，（除权得时候会重新获取）TODO
	// 每日更新-job
	public boolean spiderTodayDaliyTrade(String scode) {
		String today = DateUtil.getTodayYYYYMMDD();
		String preDate = redisUtil.get(RedisConstant.RDS_TRADE_CAL_ + today);
		try {
			JSONArray array = null;
			if (StringUtils.isBlank(scode)) {
				array = tushareSpider.getStockDaliyTrade(null, today, null, null);
			} else {
				array = tushareSpider.getStockDaliyTrade(TushareSpider.formatCode(scode), null, today, today);
			}

			if (array == null || array.size() <= 0) {
				log.warn("未获取到日交易记录,tushare,code={}");
				return false;
			}
			for (int i = 0; i < array.size(); i++) {
				TradeHistInfoDaliy d = new TradeHistInfoDaliy(array.getJSONArray(i));
				tradeHistDaliy.save(d);
				String code = d.getCode();
				String yyyymmdd = redisUtil.get(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
				if (StringUtils.isBlank(yyyymmdd)) {
					spiderDaliyTradeHistoryInfo(code);
				}
				if (StringUtils.isNotBlank(yyyymmdd) && !preDate.equals(yyyymmdd)) {
					// 补全缺失
					JSONArray array2 = tushareSpider.getStockDaliyTrade(TushareSpider.formatCode(code), null, yyyymmdd,
							today);
					if (array2 != null && array2.size() <= 0) {
						for (int ij = 0; ij < array2.size(); ij++) {
							TradeHistInfoDaliy d2 = new TradeHistInfoDaliy(array2.getJSONArray(ij));
							tradeHistDaliy.save(d2);
						}
					}
				}
				redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	public boolean spiderTodayDaliyTrade() {
		return spiderTodayDaliyTrade(null);
	}

	// 直接全量获取历史记录，不需要根据缓存来判断
	private boolean spiderDaliyTradeHistoryInfo(String code) {
		List<String> data = sinaSpider.getDaliyTradyHistory(code);
		if (data.size() <= 0) {
			log.warn("未获取到日交易记录,新浪,code={}", code);
			return false;
		}
		for (String line : data) {
			if (line.startsWith(Constant.NUM_ER)) {
				TradeHistInfoDaliy d = new TradeHistInfoDaliy(code, line);
				tradeHistDaliy.save(d);
			} else {
				log.debug(line);
			}
		}
		redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, DateUtil.getTodayYYYYMMDD());
		data = null;
		return true;
	}

	// 直接全量获取历史记录，不需要根据缓存来判断
	private boolean spiderDaliyDailyBasic() {
		String today = DateUtil.getTodayYYYYMMDD();
		String preDate = redisUtil.get(RedisConstant.RDS_TRADE_CAL_ + today);
		JSONArray array = tushareSpider.getStockDaliyBasic(null, today, null, null);
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易daily_basic（每日指标）记录,tushare");
			return false;
		}
		for (int i = 0; i < array.size(); i++) {
			DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));
			esDaliyBasicInfoDao.save(d);

			String date = redisUtil.get(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode());
			if (StringUtils.isBlank(date)) {
				// 第一次
				String json = redisUtil.get(d.getCode());
				if (StringUtils.isNotBlank(json)) {
					StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
					date = base.getList_date();
				}
				// else未更新新股
			}
			if (StringUtils.isNotBlank(date) && !preDate.equals(date)) {
				// 补全缺失
				JSONArray array2 = tushareSpider.getStockDaliyBasic(d.getTs_code(), null, date, today);
				if (array2 != null && array2.size() <= 0) {
					for (int ij = 0; ij < array2.size(); ij++) {
						DaliyBasicInfo d2 = new DaliyBasicInfo(array2.getJSONArray(ij));
						esDaliyBasicInfoDao.save(d2);
					}
				}
			}
			redisUtil.set(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode(), preDate);
		}
		return true;
	}

	/**
	 * 每日*定时任务-日交易
	 */
	public void jobSpiderAll() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("每日*定时任务-日交易[started]");
				spiderTodayDaliyTrade();
				log.info("每日*定时任务-日交易[end]");
				return null;
			}
		});
	}

	/**
	 * 手动*全部历史
	 */
	public void spiderAllDirect() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("手动*全部历史,日交易[started]");
				List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
				for (StockBaseInfo s : list) {
					redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + s.getCode());
				}
				spiderTodayDaliyTrade();
				log.info("手动*全部历史,日交易[end]");
				return null;
			}
		});
	}

	/**
	 * 每日*定时任务-除权
	 */
	public void jobSpiderAllDailyBasic() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("每日*定时任务 daily_basic [started]");
				spiderDaliyDailyBasic();
				log.info("每日*定时任务 daily_basic [end]");
				return null;
			}
		});
	}
	
	/**
	 * 每日*定时任务 
	 
	public void jobSpiderAllDailyBasic() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("每日*定时任务 daily_basic [started]");
				spiderDaliyDailyBasic();
				log.info("每日*定时任务 daily_basic [end]");
				return null;
			}
		});
	}*/
}
