package com.stable.service;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsTradeHistInfoDaliyDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.MyRunnable;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.TasksWorker2nd;
import com.stable.utils.TheadUtil;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.http.resp.DaliyTradeHistResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 日交易历史
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class DaliyTradeHistroyService {
	private static final String NEED_RELOAD = "1";
	private static final String RELOAD_OK = "0";
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsTradeHistInfoDaliyDao tradeHistDaliy;
	@Value("${python.file.market.hist.daily}")
	private String pythonFileName;
	@Autowired
	private TradeCalService tradeCalService;

	/**
	 * 手动获取日交易记录（所有）
	 */
	public void manualSpiderDaliyTrade(String code) {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL, code) {
					public Object mycall() {
						String today = DateUtil.getTodayYYYYMMDD();
						redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
						log.info("日期前复权：{}重新获取记录", code);
						String json = redisUtil.get(code);
						if (StringUtils.isNotBlank(json)) {
							StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
							spiderDaliyTradeHistoryInfoFromIPO(code, base.getList_date(), today, 0);
						}
						redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
						return null;
					}
				});
	}

	/**
	 * code除权
	 */
	public void removeCacheByChuQuan(String code) {
		String ind = redisUtil.get(RedisConstant.RDS_TRADE_Dividend_IND_ + code);
		if (StringUtils.isBlank(ind)) {
			redisUtil.set(RedisConstant.RDS_TRADE_Dividend_IND_ + code, NEED_RELOAD);
		}
	}

	// 全量获取历史记录（定时任务）-根据缓存是否需要重新获取，（除权得时候会重新获取）
	// 每日更新-job
	private boolean spiderTodayDaliyTrade() {
		String today = DateUtil.getTodayYYYYMMDD();
		return spiderTodayDaliyTrade(today);
	}

	private boolean spiderTodayDaliyTrade(String today) {
		String preDate = tradeCalService.getPretradeDate(today);
		try {
			JSONArray array = tushareSpider.getStockDaliyTrade(null, today, null, null);
			if (array == null || array.size() <= 0) {
				log.warn("未获取到日交易记录,tushare,code={}");
				return false;
			}
			for (int i = 0; i < array.size(); i++) {
				TradeHistInfoDaliy d = new TradeHistInfoDaliy(array.getJSONArray(i));
				tradeHistDaliy.save(d);
				String code = d.getCode();
				String yyyymmdd = "";
				String ind = redisUtil.get(RedisConstant.RDS_TRADE_Dividend_IND_ + code);
				if (StringUtils.isNotBlank(ind) && NEED_RELOAD.equals(ind)) {
					// 需要除权
				} else {
					yyyymmdd = redisUtil.get(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
				}

				// 第一次上市或者补全缺失
				if (StringUtils.isBlank(yyyymmdd) || !preDate.equals(yyyymmdd)) {
					log.info("日期前复权：{}重新获取记录", code);
					String json = redisUtil.get(d.getCode());
					if (StringUtils.isNotBlank(json)) {
						StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
						// spiderDaliyTradeHistoryInfoFromIPO(d.getCode(), base.getList_date(), today,
						// 0);
						// redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
						TasksWorker2nd.add(new MyRunnable() {
							@Override
							public void running() {
								spiderDaliyTradeHistoryInfoFromIPO(d.getCode(), base.getList_date(), today, 0);
								redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
								if (StringUtils.isNotBlank(ind) && NEED_RELOAD.equals(ind)) {
									redisUtil.set(RedisConstant.RDS_TRADE_Dividend_IND_ + code, RELOAD_OK,
											Duration.ofDays(10));
								}
							}
						});
					}
				} else {
					redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;

	}

	private boolean spiderDaliyTradeHistoryInfoFromIPO(String code, String startDate, String endDate, int fortimes) {
		if (fortimes >= 10) {
			log.warn("超过最大次数：code：{}，startDate：{}，endDate：{}，fortimes：{}", code, startDate, endDate, fortimes);
			return false;
		}
		fortimes++;
		TheadUtil.sleepRandomSecBetween1And30();
//		MarketHistroyVo mh = new MarketHistroyVo();
//		mh.setTs_code(TushareSpider.formatCode(code));
//		mh.setAdj("qfq");
//		mh.setStart_date(startDate);
//		mh.setEnd_date(endDate);
//		mh.setFreq("D");

//		code=sys.argv[1]
//		sdate=sys.argv[2]
//		edate=sys.argv[3]
//		padj=sys.argv[4]
//		pfreq=sys.argv[5]
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq D";
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，未获取到数据 params：{}", code, params);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
			}
			return false;
		}
		log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，获取到数据 条数：szie:{}，", code, lines.size());
		TradeHistInfoDaliy last = null;
		for (String line : lines) {
			TradeHistInfoDaliy d = this.getTradeHistInfoDaliy(line);
			if (d != null) {
				this.tradeHistDaliy.save(d);
				last = d;
			}
		}
		log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，上市日期startDate：{}，本批当前日期last：{}", code, startDate, last);
		if (last != null && !startDate.equals(last.getDate() + "")) {
			return spiderDaliyTradeHistoryInfoFromIPO(code, startDate, last.getDate() + "", fortimes);
		}
		return true;
	}

	public List<DaliyTradeHistResp> queryListByCodeByWebPage(String code, EsQueryPageReq queryPage) {
		List<DaliyTradeHistResp> res = new LinkedList<DaliyTradeHistResp>();
		List<TradeHistInfoDaliy> list = this.queryListByCode(code, queryPage);
		if (list != null) {
			for (TradeHistInfoDaliy dh : list) {
				DaliyTradeHistResp resp = new DaliyTradeHistResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<TradeHistInfoDaliy> queryListByCode(String code, EsQueryPageReq queryPage) {
		return this.queryListByCode(code, queryPage, SortOrder.DESC);
	}

	public List<TradeHistInfoDaliy> queryListByCode(String code, EsQueryPageReq queryPage, SortOrder s) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliy> page = tradeHistDaliy.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	private TradeHistInfoDaliy getTradeHistInfoDaliy(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		try {
			TradeHistInfoDaliy d = new TradeHistInfoDaliy();
			String str = line.trim().substring(1);
			String[] fv = str.split(",");
			d.setCode(TushareSpider.removets(fv[0]));
			d.setDate(Integer.valueOf(fv[1]));
			d.setOpen(Double.valueOf(fv[2]));
			d.setHigh(Double.valueOf(fv[3]));
			d.setLow(Double.valueOf(fv[4]));
			d.setClosed(Double.valueOf(fv[5]));
			d.setYesterdayPrice(Double.valueOf(fv[6]));
			d.setTodayChange(Double.valueOf(fv[7]));
			d.setTodayChangeRate(Double.valueOf(fv[8]));
			d.setVolume(Double.valueOf(fv[9]));
			try {
				d.setAmt(Double.valueOf(fv[10]));
			} catch (NumberFormatException e) {
				d.setAmt(Double.valueOf(0));
			}
			d.setId();
			return d;
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "日K数据错误", "原始数据", line);
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
//		return null;
	}

	/**
	 * 每日*定时任务-日交易
	 */
	public void jobSpiderAll() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.DAY) {
					public Object mycall() {
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
	public void spiderAllDirect(String date) {
		TasksWorker.getInstance().getService().submit(
				new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL, "手动*全部历史,日交易,date=" + date) {
					public Object mycall() {
						log.info("手动*全部历史,日交易[started]");
						List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
						for (StockBaseInfo s : list) {
							redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + s.getCode());
						}
						spiderTodayDaliyTrade(date);
						log.info("手动*全部历史,日交易[end]");
						return null;
					}
				});
	}
}
