package com.stable.service;

import java.util.Calendar;
import java.util.Date;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsDividendHistoryDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DividendHistory;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.DividendHistoryResp;
import com.stable.vo.spi.req.DividendReq;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 描述：分红送股数据
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class DividendService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsDividendHistoryDao esDividendHistoryDao;
	@Autowired
	private StockBasicService stockBasicService;
	private final String SS = "实施";
	@Autowired
	private DaliyTradeHistroyService daliydTradeHistroyService;
	@Autowired
	private RedisUtil redisUtil;

	private int spiderDividend(String ts_code, String ann_date) {
		log.info("Dividend,tushare,ts_code={},ann_date={}", ts_code, ann_date);
		List<DividendHistory> list = new LinkedList<DividendHistory>();
		try {
			DividendReq req = new DividendReq();
			if (StringUtils.isNotBlank(ts_code)) {
				req.setTs_code(ts_code);
			}
			if (StringUtils.isNotBlank(ann_date)) {
				req.setAnn_date(ann_date);
			}
			JSONArray array = tushareSpider.getDividend(req);

			if (array == null || array.size() <= 0) {
				log.warn("未获取到分红送股数据交易记录,tushare,req={}", req.toString());
				return 0;
			}
			log.info("{},{},获取到分红送股数据交易记录条数={}", ts_code, ann_date, array.size());

			// System.err.println(array);
			for (int i = 0; i < array.size(); i++) {
				JSONArray arr = array.getJSONArray(i);
				DividendHistory d = new DividendHistory(arr);
				// esDividendHistoryDao.save(d);
				list.add(d);
			}
			if (list.size() > 0) {
				esDividendHistoryDao.saveAll(list);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			if (list.size() > 0) {
				esDividendHistoryDao.saveAll(list);
			}
		}
		return list.size();
	}

	public List<DividendHistory> getListByCode(String code, String proc, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(proc)) {
			bqb.must(QueryBuilders.matchPhraseQuery("div_proc", proc));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("ex_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<DividendHistory> page = esDividendHistoryDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no DividendHistory for code={}", code);
		return null;
	}

	public List<DividendHistoryResp> getListByCodeForWebPage(String code, String proc, EsQueryPageReq querypage) {
		List<DividendHistoryResp> res = new LinkedList<DividendHistoryResp>();
		List<DividendHistory> list = this.getListByCode(code, proc, querypage);
		if (list != null) {
			for (DividendHistory dh : list) {
				DividendHistoryResp resp = new DividendHistoryResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<DividendHistory> get7DayRangeList(String start, String end) {
		Pageable pageable = PageRequest.of(0, 10000);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		// 7天左右需要除权的
		bqb.must(QueryBuilders.rangeQuery("ex_date").gte(Integer.valueOf(start)));
		bqb.must(QueryBuilders.rangeQuery("ex_date").lte(Integer.valueOf(end)));
		bqb.must(QueryBuilders.matchPhraseQuery("div_proc", SS));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<DividendHistory> page = esDividendHistoryDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	/**
	 * 前复权除权重新获取
	 */
	public void jobRespiderDaliyRecords() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND_TRADE_HISTROY, RunCycleEnum.DAY) {
					@Override
					public Object mycall() {
						String start = DateUtil.getTodayBefor7DayYYYYMMDD();
						String end = DateUtil.getTodayYYYYMMDD();
						List<DividendHistory> list = get7DayRangeList(start, end);
						if (list != null) {
							StringBuffer sb = new StringBuffer();
							for (DividendHistory d : list) {
								log.info("今日分红除权相关信息{}", d);
								daliydTradeHistroyService.removeCacheByChuQuan(d.getCode());
								redisUtil.set(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + d.getCode(),
										String.valueOf(d.getEx_date()));
								sb.append(d.getCode()).append(",");
							}
							if (sb.length() > 0) {
								WxPushUtil.pushSystem1(
										start + "-" + end + " 实施[" + list.size() + "]条分红分股！" + sb.toString());
							} else {
								WxPushUtil.pushSystem1(start + "-" + end + "无实施分红分股");
							}

						} else {
							log.info("今日无股票分红除权相关信息");
							WxPushUtil.pushSystem1("今日无实施分红分股");
						}
						return null;
					}
				});
	}

	/**
	 * 手动
	 */
	public void spiderDividendByCode(String code) {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND, RunCycleEnum.MANUAL, "code:" + code) {
					@Override
					public Object mycall() {
						int cnt = spiderDividend(TushareSpider.formatCode(code), null);
						WxPushUtil.pushSystem1(code + "获取到[" + cnt + "]条分红分股公告！");
						return null;
					}
				});
	}

	public void spiderDividendAll() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND, RunCycleEnum.MANUAL, "全部code分红分股记录") {
					@Override
					public Object mycall() {
						List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
						int cnt = 0;
						for (StockBaseInfo s : list) {
							cnt += spiderDividend(s.getTs_code(), null);
						}
						WxPushUtil.pushSystem1("All获取到[" + cnt + "]条分红分股公告！");
						return null;
					}
				});
	}

	/**
	 * 每日*定时任务-日分红
	 */
	public void jobSpiderDividendByWeek() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND, RunCycleEnum.WEEK) {
			public Object mycall() {
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				int d = 0;
				if (cal.get(Calendar.DAY_OF_WEEK) == 1) {
					d = -6;
				} else {
					d = 2 - cal.get(Calendar.DAY_OF_WEEK);
				}
				d = d - 7;// 上周
				cal.add(Calendar.DAY_OF_WEEK, d);
				int cnt = 0;
				// 所在周开始日期
				String startDate = "";
				String endDate = "";
				for (int i = 1; i <= 7; i++) {
					String date = DateUtil.getYYYYMMDD(cal.getTime());
					if (i == 1) {
						startDate = date;
					} else if (i == 7) {
						endDate = date;
					}
					log.info("每日*定时任务-日分红公告[started]:date={}", date);
					cnt += spiderDividend(null, date);
					log.info("每日*定时任务-日分红公告[end]:date={}", date);
					cal.add(Calendar.DAY_OF_WEEK, 1);
				}
				WxPushUtil.pushSystem1("上周从 " + startDate + "-" + endDate + "获取到[" + cnt + "]条分红分股公告！");
				return null;
			}
		});
	}

	/**
	 * 每日*定时任务-日分红
	 */
	public void jobSpiderDividendByDate() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND, RunCycleEnum.DAY) {
			public Object mycall() {
				String today = DateUtil.getTodayYYYYMMDD();
				log.info("每日*定时任务-日分红公告[started]");
				int cnt = spiderDividend(null, today);
				log.info("每日*定时任务-日分红公告[end]");
				WxPushUtil.pushSystem1(today + " 获取到[" + cnt + "]条分红分股公告！");
				return null;
			}
		});
	}
}
