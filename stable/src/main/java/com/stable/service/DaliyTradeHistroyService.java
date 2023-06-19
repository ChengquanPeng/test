package com.stable.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsTradeHistInfoDaliyDao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyNofqDao;
import com.stable.job.MyCallable;
import com.stable.spider.eastmoney.EastmoneyQfqSpider;
import com.stable.spider.xq.DailyFetch;
import com.stable.utils.CandlesTickChart;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.http.resp.DaliyTradeHistResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 日交易历史
 *
 */
@Service
@Log4j2
public class DaliyTradeHistroyService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsTradeHistInfoDaliyDao esTradeHistInfoDaliyDao;
	@Autowired
	private EsTradeHistInfoDaliyNofqDao esTradeHistInfoDaliyNofqDao;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private DailyFetch dailyFetch;
	@Autowired
	private TradeCalService tradeCalService;

	/**
	 * 手动获取日交易记录（所有）
	 */
	public void manualSpiderDaliyTrade(String code) {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL, code) {
					public Object mycall() {
						redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
						log.info("日期前复权：{}重新获取记录", code);
						dailyFetch.fetchByCodeAll(code, DateUtil.getTodayBefor7DayYYYYMMDD());
						return null;
					}
				});
	}

	/**
	 * code除权
	 */
	public void removeCacheByChuQuan(String code) {
		redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
		priceLifeService.removePriceLifeCache(code);
	}

	public synchronized int spiderTodayDaliyTrade(boolean isJob, String today) {
		log.info("获取日交易(分红除权)");
		int result = spiderTodayDaliyTrade(isJob, today, false);
		if (result == 0) {
			result = spiderTodayDaliyTrade(isJob, today, true);
			return result;
		} else {
			return result;
		}
	}

	private synchronized int spiderTodayDaliyTrade(boolean isJob, String today, boolean warning) {
		try {
			dailyFetch.fetchAllHushenCodes(today);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	/*
	 * private synchronized int spiderTodayDaliyTrade(boolean isJob, String today,
	 * boolean warning) { try { JSONArray array =
	 * tushareSpider.getStockDaliyTrade(null, today, null, null); if (array == null
	 * || array.size() <= 0) { log.warn("未获取到日交易记录,tushare,code={}"); if
	 * (tradeCalService.isOpen(Integer.valueOf(today))) {
	 * MsgPushServer.pushSystem1("未获取到日交易记录,tushare,日期=" + today); } return 0; }
	 * log.info("获取到日交易记录条数={}", array.size()); String preDate =
	 * tradeCalService.getPretradeDate(today); CountDownLatch cnt = new
	 * CountDownLatch(array.size()); List<TradeHistInfoDaliy> list = new
	 * LinkedList<TradeHistInfoDaliy>(); List<TradeHistInfoDaliyNofq> listNofq = new
	 * LinkedList<TradeHistInfoDaliyNofq>(); List<DaliyBasicInfo2> daliybasicList =
	 * new LinkedList<DaliyBasicInfo2>(); for (int i = 0; i < array.size(); i++) {
	 * // 1.保存记录 TradeHistInfoDaliy d = new
	 * TradeHistInfoDaliy(array.getJSONArray(i)); String code = d.getCode(); try {
	 * int qfqDate =
	 * Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code,
	 * "0")); d.setQfqDate(qfqDate); list.add(d); TradeHistInfoDaliyNofq nofq = new
	 * TradeHistInfoDaliyNofq(array.getJSONArray(i)); listNofq.add(nofq); if (isJob)
	 * { DaliyBasicInfo2 dalyb = new DaliyBasicInfo2(array.getJSONArray(i));
	 * daliybasicList.add(dalyb); }
	 * 
	 * // 2.是否需要更新缺失记录 String yyyymmdd =
	 * redisUtil.get(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code); if
	 * (StringUtils.isBlank(yyyymmdd) || (!preDate.equals(yyyymmdd) &&
	 * !yyyymmdd.equals(today) && Integer.valueOf(yyyymmdd) <
	 * Integer.valueOf(today))) {//
	 * log.info("代码code:{}重新获取记录->redis-last:{},preDate:{},today:{},index={}", code,
	 * yyyymmdd, preDate, today, i); String json = redisUtil.get(d.getCode()); //
	 * 第一次上市或者除权 StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
	 * if (base != null) { TasksWorker2nd.add(new TasksWorker2ndRunnable() { public
	 * void running() { try { spiderDaliyTradeHistoryInfoFromIPOCenter(code, today,
	 * 0); spiderDaliyTradeHistoryInfoFromIPOCenterNofq(code, 0); } catch (Exception
	 * e) { MsgPushServer.pushSystem1("重新获取前后复权出错：" + code); } finally {
	 * cnt.countDown(); } } }); } else { log.info("代码code:{} 未获取到StockBaseInfo",
	 * code); cnt.countDown(); } } else {
	 * log.info("代码:{},不需要重新更新记录,上个交易日期 preDate:{},上次更新日期:{},最后更新日期:{},index={}",
	 * code, preDate, yyyymmdd, today, i);
	 * redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
	 * priceLifeService.checkAndSetPrice(d); cnt.countDown(); } } catch (Exception
	 * e) { e.printStackTrace(); MsgPushServer.pushSystem1("前复权qfq获取异常==>代码:" +
	 * code); cnt.countDown(); } } try { if (!cnt.await(12, TimeUnit.HOURS)) {
	 * MsgPushServer.pushSystem1("前复权qfq获取超时异常==>日期:" + today); } } catch
	 * (InterruptedException e) { e.printStackTrace(); } if (daliybasicList.size() >
	 * 0) { esDaliyBasicInfoDao.saveAll(daliybasicList); } if (listNofq.size() > 0)
	 * { esTradeHistInfoDaliyNofqDao.saveAll(listNofq); } if (list.size() > 0) {
	 * esTradeHistInfoDaliyDao.saveAll(list); } // 定时任务
	 * ThreadsUtil.sleepRandomSecBetween15And30(); if (isJob) { if
	 * (daliybasicList.size() > 0) { // tickService.genTickEveryDay(daliybasicList,
	 * dddd); xqDailyBaseSpider.fetchAll(daliybasicList, listNofq); } } return
	 * list.size(); } catch (Exception e) { e.printStackTrace();
	 * log.error(e.getMessage(), e); if (warning) {
	 * MsgPushServer.pushSystem1("前复权qfq获取异常，获取日期:" + today); } } return 0;
	 * 
	 * }
	 */
	public List<DaliyTradeHistResp> queryListByCodeByWebPage(String code, EsQueryPageReq queryPage) {
		List<DaliyTradeHistResp> res = new LinkedList<DaliyTradeHistResp>();
		List<TradeHistInfoDaliy> list = this.queryListByCodeQfq(code, 0, 0, queryPage, SortOrder.DESC);
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

	public List<TradeHistInfoDaliy> queryListByCodeWithLastQfq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		int qfqDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
		List<TradeHistInfoDaliy> db = queryListByCodeQfq(code, startDate, endDate, queryPage, s);
		boolean needFetch = false;
//		log.info("{},qfqDate={}", code, qfqDate);
//		log.info("db is null =" + (db == null));
//		log.info("db.size =" + (db != null ? db.size() : 0));
//		log.info("queryPage.getPageSize=" + queryPage.getPageSize());
//		log.info("SortOrder=" + s.toString());
		if (db == null || db.size() <= 0 || (queryPage.getPageSize() < 9999 && db.size() != queryPage.getPageSize())) {
			log.info(code + " needFetch=true");
			needFetch = true;
		} else {
			if (qfqDate != 0) {
				for (TradeHistInfoDaliy r : db) {
					if (r.getQfqDate() < qfqDate) {// 存的数据是前复权日期版本小于redis，不是最新的
						log.info("{},TradeHistInfoDaliy date={},QfqDate={}", code, r.getDate(), r.getQfqDate());
						needFetch = true;
						break;
					}
				}
			}
			// 数据是否正确
			if (!needFetch && queryPage.getPageSize() < 9999) {
				if (s == SortOrder.DESC) {
					if (endDate != 0 && db.get(0).getDate() != endDate) {
						log.info(code + " endDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(db.size() - 1).getDate() != startDate) {
						log.info(code + " startDate={},db-last={}", startDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
				} else {
					if (endDate != 0 && db.get(db.size() - 1).getDate() != endDate) {
						log.info(code + " endDate={},db-last={}", endDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(0).getDate() != startDate) {
						log.info(code + " startDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
				}
			}
		}

		if (needFetch) {
			log.info("code={},startDate={},endDate={},queryPage={},SortOrder={}", code, startDate, endDate,
					queryPage.getPageSize(), s.toString());
//			new Exception().printStackTrace();
			String today = DateUtil.getTodayYYYYMMDD();
			if (dailyFetch.spiderDaliyTradeHistoryInfoFromIPOCenter(code, today, 0)) {
				return queryListByCodeQfq(code, startDate, endDate, queryPage, s);
			}
			return null;
		}
		return db;
	}

	public List<TradeHistInfoDaliy> queryListByCodeQfq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}
		if (endDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").lte(endDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliy> page = esTradeHistInfoDaliyDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<TradeHistInfoDaliyNofq> queryListByCodeWithLastNofq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		List<TradeHistInfoDaliyNofq> db = queryListByCodeNofq(code, startDate, endDate, queryPage, s);
		boolean needFetch = false;
//		log.info("{},qfqDate={}", code, qfqDate);
//		log.info("db is null =" + (db == null));
//		log.info("db.size =" + (db != null ? db.size() : 0));
//		log.info("queryPage.getPageSize=" + queryPage.getPageSize());
//		log.info("SortOrder=" + s.toString());
		if (db == null || db.size() <= 0 || (queryPage.getPageSize() < 9999 && db.size() != queryPage.getPageSize())) {
			log.info(code + " needFetch=true");
			needFetch = true;
		} else {
			// 数据是否正确
			if (queryPage.getPageSize() < 9999) {
				if (s == SortOrder.DESC) {
					if (endDate != 0 && db.get(0).getDate() != endDate) {
						log.info(code + " endDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(db.size() - 1).getDate() != startDate) {
						log.info(code + " startDate={},db-last={}", startDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
				} else {
					if (endDate != 0 && db.get(db.size() - 1).getDate() != endDate) {
						log.info(code + " endDate={},db-last={}", endDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(0).getDate() != startDate) {
						log.info(code + " startDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
				}
			}
		}

		if (needFetch) {
			log.info("needFetch Nofq code={},startDate={},endDate={},queryPage={},SortOrder={}", code, startDate,
					endDate, queryPage.getPageSize(), s.toString());
//			new Exception().printStackTrace();
			if (dailyFetch.spiderDaliyTradeHistoryInfoFromIPOCenterNofq(code, 0)) {
				return queryListByCodeNofq(code, startDate, endDate, queryPage, s);
			}
			return null;
		}
		return db;
	}

	public List<TradeHistInfoDaliyNofq> queryListByCodeNofq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}
		if (endDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").lte(endDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliyNofq> page = esTradeHistInfoDaliyNofqDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	/**
	 * 手动*全部历史
	 */
	public void spiderAllDirect() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL, "手动*全部历史,日交易,date=") {
					public Object mycall() {
						log.info("手动*全部历史,日交易[started]");
						List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithSort();
						for (StockBaseInfo s : list) {
							redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + s.getCode());
						}
						int date = DateUtil.getTodayIntYYYYMMDD();
						if (!tradeCalService.isOpen(date)) {
							date = tradeCalService.getPretradeDate(date);
						}
						spiderTodayDaliyTrade(false, date + "");
						log.info("手动*全部历史,日交易[end]");
						return null;
					}
				});
	}

	public TradeHistInfoDaliyNofq queryLastNofq(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (date > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		try {
			return esTradeHistInfoDaliyNofqDao.search(sq).getContent().get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public TradeHistInfoDaliyNofq queryLastNofq(String code) {
		return queryLastNofq(code, 0);
	}

	public TradeHistInfoDaliy queryLastfq(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (date > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		try {
			return esTradeHistInfoDaliyDao.search(sq).getContent().get(0);
		} catch (Exception e) {
			return null;
		}
	}

	// 一年新高的价格（前复权）
//	private TradeHistInfoDaliy queryYear1HighRecord(String code, int date) {
//		List<TradeHistInfoDaliy> list = queryListByCodeWithLastQfq(code, 0, date, EsQueryPageUtil.queryPage250,
//				SortOrder.DESC);
//		if (list != null && list.size() > 0) {
//			return list.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
//		}
//		return new TradeHistInfoDaliy();
//	}

	public TradeHistInfoDaliy queryMonth3HighRecord(String code, int date) {
		List<TradeHistInfoDaliy> list = queryListByCodeWithLastQfq(code, 0, date, EsQueryPageUtil.queryPage60,
				SortOrder.DESC);
		if (list != null && list.size() > 0) {
			return list.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		}
		return new TradeHistInfoDaliy();
	}

	public TradeHistInfoDaliy queryLastfq(String code) {
		return queryLastfq(code, 0);
	}

	public static void main(String[] args) {
		new DaliyTradeHistroyService().imagesLocal();
	}

//	@javax.annotation.PostConstruct
	public void imagesLocal() {
		String code = "300414";
		int days = 5;
		String filePath = "C:/Users/roy/Desktop/" + code + ".jpg";
		List<TradeHistInfoDaliy> all = EastmoneyQfqSpider.getQfq(code);
		List<TradeHistInfoDaliy> t = new LinkedList<>();
		int i = 0;
		for (int j = all.size() - 1; j > 0; j--) {
			TradeHistInfoDaliy td = all.get(j);
			// System.err.println(td);
			t.add(td);
			i++;
			if (i >= 120) {
				break;
			}
		}
		images(code, code, DateUtil.getTodayIntYYYYMMDD(), days, filePath, t);
		System.exit(0);
	}

	public void imagesServer(String code, int endDate, int days, String filePath) {
		List<TradeHistInfoDaliy> t = this.queryListByCodeQfq(code, 0, endDate, EsQueryPageUtil.queryPage120,
				SortOrder.DESC);
		images(code, stockBasicService.getCodeName2(code), endDate, days, filePath, t);
	}

	private void images(String code, String title, int endDate, int days, String filePath, List<TradeHistInfoDaliy> t) {
		try {
			List<TradeHistInfoDaliy> bars = new LinkedList<TradeHistInfoDaliy>();
			bars.addAll(t);

			Set<Integer> map = new HashSet<Integer>();
			for (TradeHistInfoDaliy bar : bars) {
				map.add(bar.getDate());
			}
			// 所有非交易日
			List<Date> allNonTradedays = new ArrayList<Date>();
			Date tmp = DateUtil.parseDate(bars.get(bars.size() - 1).getDate());
			int end = bars.get(0).getDate();
			if (end > bars.get(bars.size() - 1).getDate()) {
				while (true) {
					int dt = DateUtil.formatYYYYMMDDReturnInt(tmp);
					if (dt == end) {
						break;
					}
					if (!map.contains(dt)) {
						allNonTradedays.add(tmp);
					}
					tmp = DateUtil.addDate(tmp, 1);
				}
			}

			for (int i = 0; i < days; i++) {
				TradeHistInfoDaliy la = bars.get(0);
				TradeHistInfoDaliy id = new TradeHistInfoDaliy();

				int tmp2 = DateUtil.addDate(la.getDate(), 1);
				if (DateUtil.isWeekend(tmp2)) {
					tmp2 = DateUtil.addDate(tmp2, 1);
				}
				if (DateUtil.isWeekend(tmp2)) {
					tmp2 = DateUtil.addDate(tmp2, 1);
				}
				id.setDate(tmp2);
				id.setOpen(la.getClosed());
				id.setClosed(CurrencyUitl.topPrice(la.getClosed(), false));
				id.setHigh(id.getClosed());
				id.setLow(la.getClosed());
				id.setVolume(la.getVolume() * 1.25);
				bars.add(0, id);
				System.err.println(id);
			}
			// 有两根重叠的现象是因为交易日期的问题。
			CandlesTickChart.strt(filePath, title, bars, allNonTradedays);
			// ImageUtil.generateImages(filePath, data);
			// System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
