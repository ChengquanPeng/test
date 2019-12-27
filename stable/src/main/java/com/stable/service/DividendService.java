package com.stable.service;

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
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.EsDividendHistoryDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.TasksWorker;
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

	private boolean spiderDividend(String ts_code, String ann_date) {
		log.warn("Dividend,tushare,ts_code={},ann_date={}", ts_code, ann_date);
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
				log.warn("未获取到日交易记录,tushare,code={}");
				return false;
			}
			// System.err.println(array);
			for (int i = 0; i < array.size(); i++) {
				JSONArray arr = array.getJSONArray(i);
				// if (SS.equals(arr.get(3))) {
				// System.err.println(arr);
				DividendHistory d = new DividendHistory(arr);
				// System.err.println(d);
				esDividendHistoryDao.save(d);
				// }
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	public List<DividendHistory> getListByCode(String code, String proc, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(proc)) {
			bqb.must(QueryBuilders.matchPhraseQuery("div_proc", proc));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("end_date").unmappedType("integer").order(SortOrder.DESC);

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

	public List<DividendHistory> getTodayListByCode() {
		Pageable pageable = PageRequest.of(0, 10000);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("ex_date", Integer.valueOf(DateUtil.getTodayYYYYMMDD())));
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
						List<DividendHistory> list = getTodayListByCode();
						if (list != null) {
							for (DividendHistory d : list) {
								log.info("今日分红除权相关信息{}", d);
								daliydTradeHistroyService.removeCacheByChuQuan(d.getCode());
							}
						} else {
							log.info("今日无股票分红除权相关信息");
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
						spiderDividend(TushareSpider.formatCode(code), null);
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
						for (StockBaseInfo s : list) {
							spiderDividend(s.getTs_code(), null);
						}
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
				log.info("每日*定时任务-日分红公告[started]");
				spiderDividend(null, DateUtil.getTodayYYYYMMDD());
				log.info("每日*定时任务-日分红公告[end]");
				return null;
			}
		});
	}
}
