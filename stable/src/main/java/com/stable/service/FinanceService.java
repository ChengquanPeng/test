package com.stable.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.stable.es.dao.base.EsFinanceBaseInfoHyDao;
import com.stable.job.MyCallable;
import com.stable.service.model.CodeModelService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.spider.ths.ThsHolderSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.TasksWorker;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.FinanceBaseInfoHangye;
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
	private EsFinanceBaseInfoHyDao esFinanceBaseInfoHyDao;
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
	@Autowired
	private ZhiYaService zhiYaService;
	@Autowired
	private ThsHolderSpider thsHolderSpider;
	@Autowired
	private ConceptService conceptService;

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
				WxPushUtil.pushSystem1("未从东方财富抓取到Finane记录,code=" + code);
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

	public FinanceBaseInfo getLastFinaceReport(String code, int year, int quarter) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		bqb.must(QueryBuilders.matchPhraseQuery("quarter", quarter));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no last report fince code={},year={},quarter={}", code, year, quarter);
		return null;

	}

	public FinanceBaseInfo getLastFinaceReport(String code, int annDate) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("annDate").lte(annDate));// 最新公告
		FieldSortBuilder sort = SortBuilders.fieldSort("annDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
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
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<FinYjyg> page = esFinYjygDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
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
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<FinYjkb> page = esFinYjkbDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
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
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
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
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

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
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

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
						fetchFinances();
						return null;
					}
				});
	}

	public void byJob() {
		int date = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		log.info("模型开始之前运行执行：1.质押，2.股东人数");
		zhiYaService.fetchBySun();
		thsHolderSpider.dofetchHolder();
		fetchFinances();
		executeHangye(date);
		// 运行完财务和行业对比后,重新运行
		codeModelService.runJobv2(true, date);
	}

	private List<FinanceBaseInfoHangye> executeHangye(int date) {
		log.info("行业对比开始");
		cache = new HashMap<String, FinanceBaseInfoHangye>();
		List<FinanceBaseInfoHangye> hys = new LinkedList<FinanceBaseInfoHangye>();
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : list) {
			String code = s.getCode();
			try {
				List<CodeConcept> cc = conceptService.getCodeConcept(code, 2);// 同花顺行业
				if (cc != null && cc.size() > 0) {
					CodeConcept c = cc.get(0);
					log.info("code={},ConceptId={},ConceptName={}", code, c.getConceptId(), c.getConceptName());
					List<CodeConcept> allcode = conceptService.getCodes(c.getConceptId());
					FinanceBaseInfo fbi = this.getLastFinaceReport(code);
					if (fbi != null) {
						if (!getFromCache(code, fbi.getYear(), fbi.getQuarter())) {// from chace
							executeHangyeExt1(date, fbi.getYear(), fbi.getQuarter(), allcode, hys, c.getConceptId(),
									c.getConceptName());
						}
					}
				}
			} catch (Exception e) {
				WxPushUtil.pushSystem1("行业分析（毛利率，应收占款）计算异常:" + code);
				ErrorLogFileUitl.writeError(e, "行业分析（毛利率，应收占款）计算异常:", "", "");
			}
		}
		if (hys.size() > 0) {
			esFinanceBaseInfoHyDao.saveAll(hys);
		}
		log.info("行业对比结束");
		return hys;
	}

	private boolean getFromCache(String code, int year, int quarter) {
		String key = code + year + "" + quarter;
		if (cache.containsKey(key)) {
			return true;
		}
		return false;
	}

	private Map<String, FinanceBaseInfoHangye> cache = new HashMap<String, FinanceBaseInfoHangye>();

	private void executeHangyeExt1(int updateDate, int year, int quarter, List<CodeConcept> allcode,
			List<FinanceBaseInfoHangye> hys, String hyid, String hyName) {
		List<FinanceBaseInfo> rl = new LinkedList<FinanceBaseInfo>();
		double mll = 0.0;
		int mllc = 0;

		List<FinanceBaseInfo> yszkl = new LinkedList<FinanceBaseInfo>();
		double yszk = 0.0;
		int yszkc = 0;
		for (CodeConcept c : allcode) {
			log.info("板块：{},code={}", c.getConceptName(), c.getCode());
			FinanceBaseInfo f = this.getLastFinaceReport(c.getCode(), year, quarter);
			if (f != null) {
				if (f.getMll() != 0) {
					rl.add(f);
					mll += f.getMll();
					mllc++;
				}
				if (f.getAccountrecRatio() != 0) {
					yszkl.add(f);
					yszk += f.getAccountrecRatio();
					yszkc++;
				}
			}
		}
		// ====毛利率====start====
		double avgtMll = 0.0;
		if (mllc > 0) {
			avgtMll = CurrencyUitl.roundHalfUp(mll / (double) mllc);
		}
		mllSort(rl);
		for (int i = 0; i < rl.size(); i++) {// 有数据的
			FinanceBaseInfo r = rl.get(i);
			FinanceBaseInfoHangye hy = new FinanceBaseInfoHangye();
			hy.setCode(r.getCode());
			hy.setMll(r.getMll());
			hy.setMllAvg(avgtMll);
			hy.setMllRank((i + 1));
			hy.setYear(year);
			hy.setQuarter(quarter);
			hy.setId(hy.getCode() + year + "" + quarter);
			hy.setHangyeId(hyid);
			hy.setHangyeName(hyName);
			hy.setUpdateDate(updateDate);
			cache.put(hy.getId(), hy);
			hys.add(hy);
		}
		for (CodeConcept c : allcode) {// 无数据的：补全
			String key = c.getCode() + year + "" + quarter;
			if (!cache.containsKey(key)) {
				FinanceBaseInfoHangye hy = new FinanceBaseInfoHangye();
				hy.setCode(c.getCode());
				hy.setMll(0);
				hy.setMllAvg(avgtMll);
				hy.setMllRank(9999);
				hy.setYear(year);
				hy.setQuarter(quarter);
				hy.setId(key);
				hy.setHangyeId(hyid);
				hy.setHangyeName(hyName);
				hy.setUpdateDate(updateDate);
				cache.put(hy.getId(), hy);
				hys.add(hy);
			}
		}
		// ====毛利率====end====

		// ====应收占款比率====start====
		double avgtAr = 0.0;
		if (yszkc > 0) {
			avgtAr = CurrencyUitl.roundHalfUp(yszk / (double) yszkc);
		}
		arSort(yszkl);
		for (int i = 0; i < yszkl.size(); i++) {
			FinanceBaseInfo r = rl.get(i);
			String key = r.getCode() + year + "" + quarter;
			FinanceBaseInfoHangye hy = cache.get(key);
			hy.setYszk(r.getAccountrecRatio());
			hy.setYszkRank((i + 1));
			hy.setYszkAvg(avgtAr);
		}
		for (CodeConcept c : allcode) {
			String key = c.getCode() + year + "" + quarter;
			FinanceBaseInfoHangye hy = cache.get(key);
			hy.setYszk(0);
			hy.setYszkRank(9999);
			hy.setYszkAvg(avgtAr);
		}
		// ====应收占款比率====end====
	}

	// 毛利率倒序排序
	private void mllSort(List<FinanceBaseInfo> rl) {
		Collections.sort(rl, new Comparator<FinanceBaseInfo>() {
			@Override
			public int compare(FinanceBaseInfo o1, FinanceBaseInfo o2) {
				if (o1.getMll() == o2.getMll()) {
					return 0;
				}
				return o2.getMll() - o1.getMll() > 0 ? 1 : -1;
			}
		});
	}

	// 应收占款比率倒序排序
	private void arSort(List<FinanceBaseInfo> rl) {
		Collections.sort(rl, new Comparator<FinanceBaseInfo>() {
			@Override
			public int compare(FinanceBaseInfo o1, FinanceBaseInfo o2) {
				if (o1.getAccountrecRatio() == o2.getAccountrecRatio()) {
					return 0;
				}
				return o2.getAccountrecRatio() - o1.getAccountrecRatio() > 0 ? 1 : -1;
			}
		});
	}

	private void fetchFinances() {
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
