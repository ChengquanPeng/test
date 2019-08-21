package com.stable.service;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.Constant;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.EsTradeHistInfoDaliyDao;
import com.stable.spider.sina.SinaSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TradeHistroyService {
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

	// 全量获取历史记录（定时任务）-根据缓存是否需要重新获取，（除权得时候会重新获取）TODO
	// 每日更新-job
	public boolean spiderTodayDaliyTrade(String code) {
		String yyyymmdd = redisUtil.get(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
		// redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, "20190101");
		if (StringUtils.isBlank(yyyymmdd)) {
			return spiderDaliyTradeHistoryInfo(code);
		}
		try {
			JSONArray array = tushareSpider.getStockDaliyTrade(TushareSpider.formatCode(code), yyyymmdd,
					DateUtil.getTodayYYYYMMDD());
			if (array == null || array.size() <= 0) {
				log.warn("未获取到日交易记录,tushare,code={}", code);
				return false;
			}
			for (int i = 0; i < array.size(); i++) {
				TradeHistInfoDaliy d = new TradeHistInfoDaliy(code, array.getJSONArray(i));
				tradeHistDaliy.save(d);
			}
			redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, DateUtil.getTodayYYYYMMDD());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
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

	/**
	 * 每日*定时任务
	 */
	public void jobSpiderAll() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
				for (StockBaseInfo s : list) {
					spiderTodayDaliyTrade(s.getCode());
				}
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
				List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
				for (StockBaseInfo s : list) {
					redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + s.getCode());
					spiderTodayDaliyTrade(s.getCode());
				}
				return null;
			}
		});
	}
}
