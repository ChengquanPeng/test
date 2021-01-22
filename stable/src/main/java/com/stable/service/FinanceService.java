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
import com.stable.es.dao.base.EsFinYjkbDao;
import com.stable.es.dao.base.EsFinYjygDao;
import com.stable.es.dao.base.EsFinanceBaseInfoDao;
import com.stable.job.MyCallable;
import com.stable.service.model.CodeModelService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.FinanceBaseInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 财务
 *
 */
@Service
@Log4j2
public class FinanceService {

	@Autowired
	private EsFinanceBaseInfoDao esFinanceBaseInfoDao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsFinYjygDao esFinYjygDao;
	@Autowired
	private EsFinYjkbDao esFinYjkbDao;
	@Autowired
	private EastmoneySpider eastmoneySpider;
	@Autowired
	private CodeModelService codeModelService;

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
				resp.setJqjzcsyl(dh.getJqjzcsyl());
				resp.setMgjyxjl(dh.getMgjyxjl());
				resp.setMll(dh.getMll());
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

	Pageable pageable = PageRequest.of(0, 1);

	public FinanceBaseInfo getLastFinaceReport(String code, int annDate) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("annDate").lte(annDate));// 最新公告
		FieldSortBuilder sort = SortBuilders.fieldSort("annDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinanceBaseInfo f = page.getContent().get(0);
			log.info("page size={},last report fince code={},date={}", page.getContent().size(), code, f.getDate());
			return f;
		}
		log.info("no last report fince code={},annDate={}", code, annDate);
		return null;
	}

	public FinYjyg getLastFinYjygReport(String code, int annDate, int year, int jidu) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("annDate").lte(annDate));//
		bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		bqb.must(QueryBuilders.matchPhraseQuery("quarter", jidu));
		bqb.must(QueryBuilders.matchPhraseQuery("isValid", 1));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinYjyg> page = esFinYjygDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinYjyg f = page.getContent().get(0);
			// log.info("page size={},getLastFinYjkbReport code={},date={}",
			// page.getContent().size(), code, f.getDate());
			return f;
		}
		log.info("no FinYjyg report fince code={},annDate={}", code, annDate);
		return null;
	}

	public FinYjkb getLastFinYjkbReport(String code, int annDate, int year, int jidu) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("annDate").lte(annDate));//
		bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		bqb.must(QueryBuilders.matchPhraseQuery("quarter", jidu));
		bqb.must(QueryBuilders.matchPhraseQuery("isValid", 1));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinYjkb> page = esFinYjkbDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinYjkb f = page.getContent().get(0);
			// log.info("page size={},getLastFinYjkbReport code={},date={}",
			// page.getContent().size(), code, f.getDate());
			return f;
		}
		log.info("no FinYjkb report fince code={},annDate={}", code, annDate);
		return null;
	}

	Pageable pageable4 = PageRequest.of(0, 4);

	/**
	 * 最近4个季度
	 */
	public List<FinanceBaseInfo> getLastFinaceReport4Quarter(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable4).build();
		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
			// log.info("page size={},last report fince code={},date={}",
			// page.getContent().size(), code, f.getDate());
		}
		log.info("no last report fince code={},now!", code);
		return null;
	}

	public FinanceBaseInfo getLastFinaceReport(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinanceBaseInfo f = page.getContent().get(0);
			// log.info("page size={},last report fince code={},date={}",
			// page.getContent().size(), code, f.getDate());
			return f;
		}
		log.info("no last report fince code={},now!", code);
		return null;
	}

	public FinYjkb getLastFinaceKbByReportDate(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("isValid", 1));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinYjkb> page = esFinYjkbDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	public FinYjyg getLastFinaceYgByReportDate(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("isValid", 1));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinYjyg> page = esFinYjygDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	public void jobSpiderKuaiYuBao() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.FINACE_FRIST, RunCycleEnum.DAY) {
			public Object mycall() {
				log.info("同步业绩预报和快报[started]");
				int updateDate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
				List<FinYjkb> list1 = eastmoneySpider.getFinYjkb();
				List<FinYjyg> list2 = eastmoneySpider.getFinYjyg();
				StringBuffer sb = new StringBuffer();
				int index = list1.size();
				if (list1.size() > 0) {
					for (FinYjkb fy : list1) {
						if (index <= 5) {// 前面5条
							sb.append(stockBasicService.getCodeName(fy.getCode())).append(",");
						}
						fy.setUpdateDate(updateDate);
						fy.setIsValid(1);
						index--;
					}
					esFinYjkbDao.saveAll(list1);
				}
				index = list2.size();
				if (list2.size() > 0) {
					for (FinYjyg fy : list2) {// 前面5条
						if (index <= 5) {
							sb.append(stockBasicService.getCodeName(fy.getCode())).append(",");
						}
						fy.setUpdateDate(updateDate);
						fy.setIsValid(1);
						index--;
					}
					esFinYjygDao.saveAll(list2);
				}
				log.info("同步业绩预报和快报[end]");

				WxPushUtil.pushSystem1(
						"同步业绩预报和快报完成！" + (sb.length() > 0 ? ("今日快报或者预告:" + sb.toString()) : "今日无业绩快报或者预告"));
				return null;
			}
		});

	}

	public void byWeb() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.FINACE_HISTORY, RunCycleEnum.WEEK) {
					public Object mycall() {
						fetch();
						return null;
					}
				});
	}

	public void byJob() {
		fetch();
		executeHangye();
		// 运行完财务和行业对比后,重新运行
		codeModelService.runJob(true, Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
	}

	private void executeHangye() {
		// TODO
	}

	private void fetch() {
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
		WxPushUtil.pushSystem1("同步股票财务报告完成！股票总数：[" + total + "],成功股票数[" + cnt + "],失败股票数=" + (total - cnt));
	}
}
