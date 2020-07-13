package com.stable.service;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsFinanceBaseInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.TasksWorker;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.FinanceBaseInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 财务
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class FinanceService {

	@Autowired
	private EsFinanceBaseInfoDao esFinanceBaseInfoDao;
	@Autowired
	private StockBasicService stockBasicService;

	/**
	 * 删除redis，从头开始获取
	 */
	public boolean spiderFinaceHistoryInfoFromStart(String code) {
		List<FinanceBaseInfo> list = new LinkedList<FinanceBaseInfo>();
		if (spiderFinaceHistoryInfo(code, list)) {
			if (list.size() > 0) {
				esFinanceBaseInfoDao.saveAll(list);
			}
			return true;
		}
		return false;
	}

	private boolean spiderFinaceHistoryInfo(String code, List<FinanceBaseInfo> list) {
		try {

			List<FinanceBaseInfo> datas = EastmoneySpider.getNewFinanceAnalysis(code, 0);// 0按报告期、1=年报
			if (datas == null || datas.size() <= 0) {
				log.warn("未从东方财富抓取到Finane记录,code={}", code);
				return false;
			}
			log.warn("从东方财富抓取到Finane记录{}条,code={}", datas.size(), code);
			list.addAll(datas);
		} finally {
			ThreadsUtil.sleepRandomSecBetween1And5();
		}
		return true;
	}

	public List<FinanceBaseInfo> getFinaceReports(String code, String year, String quarter, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(year)) {
			bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		}
		if (StringUtils.isNotBlank(quarter)) {
			bqb.must(QueryBuilders.matchPhraseQuery("quarter", quarter));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<FinanceBaseInfoResp> getListByCodeForWebPage(String code, String year, String quarter,
			EsQueryPageReq queryPage) {
		List<FinanceBaseInfoResp> res = new LinkedList<FinanceBaseInfoResp>();
		List<FinanceBaseInfo> list = this.getFinaceReports(code, year, quarter, queryPage);
		if (list != null) {
			for (FinanceBaseInfo dh : list) {
				FinanceBaseInfoResp resp = new FinanceBaseInfoResp();
				resp.setCode(dh.getCode());
				resp.setEndDate(String.valueOf(dh.getDate()));
				resp.setEndType(dh.getYear(), dh.getQuarter());
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setYyzsr(CurrencyUitl.covertToString(dh.getYyzsr()));
				resp.setGsjlr(CurrencyUitl.covertToString(dh.getGsjlr()));
				resp.setKfjlr(CurrencyUitl.covertToString(dh.getKfjlr()));
				resp.setYyzsrtbzz(dh.getYyzsrtbzz());
				resp.setGsjlrtbzz(dh.getGsjlrtbzz());
				resp.setKfjlrtbzz(dh.getKfjlrtbzz());
				res.add(resp);
			}
		}
		return res;
	}

	public FinanceBaseInfo getFinaceReportByLteDate(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));// 报告期
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no last report fince date={}", date);
		return null;
	}

	public List<FinanceBaseInfo> getFinacesReportByLteDate(String code, int date, EsQueryPageReq queryPage8) {
		Pageable pageable = PageRequest.of(queryPage8.getPageNum(), queryPage8.getPageSize());
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));// 报告期
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no last report fince date={}", date);
		return null;
	}

	public FinanceBaseInfo getLastFinaceReport(String code) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinanceBaseInfo f = page.getContent().get(0);
			log.info("page size={},last report fince code={},date={}", page.getContent().size(), code, f.getDate());
			return f;
		}
		log.info("no last report fince code={}", code);
		return null;
	}

	public void jobSpiderFinaceHistoryInfo() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.FINACE_HISTORY, RunCycleEnum.WEEK) {
					public Object mycall() {
						log.info("同步财务报告报告[started]");
						List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
						int total = list.size();
						log.info("股票总数：" + total);
						List<FinanceBaseInfo> rl = new LinkedList<FinanceBaseInfo>();
						int cnt = 0;
						for (StockBaseInfo s : list) {
							if (spiderFinaceHistoryInfo(s.getCode(), rl)) {
								cnt++;
							}
							if (rl.size() > 1000) {
								esFinanceBaseInfoDao.saveAll(rl);
								rl = new LinkedList<FinanceBaseInfo>();
							}
						}
						if (rl.size() > 0) {
							esFinanceBaseInfoDao.saveAll(rl);
						}
						log.info("同步财务报告报告[end]");
						WxPushUtil.pushSystem1(
								"同步股票财务报告完成！股票总数：[" + total + "],成功股票数[" + cnt + "],失败股票数=" + (total - cnt));
						return null;
					}
				});
	}
}
