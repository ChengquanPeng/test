package com.stable.service.model;

import java.util.Date;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsCodeBaseModelDao;
import com.stable.es.dao.base.EsCodeBaseModelHistDao;
import com.stable.service.BuyBackService;
import com.stable.service.ChipsService;
import com.stable.service.CodePoolService;
import com.stable.service.ConceptService;
import com.stable.service.DividendService;
import com.stable.service.FinanceService;
import com.stable.service.PlateService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.data.FinanceAnalyzer;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.RedisUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BonusHist;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.CodePool;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZhiYa;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelService {
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private DividendService dividendService;
	@Autowired
	private BuyBackService buyBackService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ZhiYaService zhiYaService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsCodeBaseModelDao codeBaseModelDao;
	@Autowired
	private EsCodeBaseModelHistDao codeBaseModelHistDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private CoodPoolModelService middleSortV1Service;
	@Autowired
	private PlateService plateService;
	@Autowired
	private ChipsService chipsService;

	public synchronized void runJob(boolean isJob, int date) {
		try {
			runByJob(date);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "CodeModel模型运行异常", "", "");
			WxPushUtil.pushSystem1("CodeModel模型运行异常..");
		}
	}

	private int start = 0;

	private synchronized void runByJob(int tradeDate) {
		Date now = new Date();
		start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));
//		end = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, 370));
		log.info("CodeModel processing request date={}", tradeDate);
		if (!tradeCalService.isOpen(tradeDate)) {
			tradeDate = tradeCalService.getPretradeDate(tradeDate);
		}
		log.info("Actually processing request date={}", tradeDate);
		int updatedate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		List<CodeBaseModel> listLast = new LinkedList<CodeBaseModel>();
		List<CodeBaseModelHist> listHist = new LinkedList<CodeBaseModelHist>();
		int oneYearAgo = DateUtil.getPreYear(tradeDate);
		int nextYear = DateUtil.getNextYear(tradeDate);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		Map<String, CodeBaseModel> histMap = getALLForMap();
		Map<String, CodePool> map = codePoolService.getCodePoolMap();
		List<CodePool> list = new LinkedList<CodePool>();
		for (StockBaseInfo s : codelist) {
			try {
				getSorce(s, tradeDate, oneYearAgo, nextYear, updatedate, listLast, listHist, true,
						histMap.get(s.getCode()), list, map);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
			}
		}
		if (listLast.size() > 0) {
			codeBaseModelDao.saveAll(listLast);
		}
		if (listHist.size() > 0) {
			codeBaseModelHistDao.saveAll(listHist);
		}
		middleSortV1Service.start(tradeDate, list);
		log.info("CodeModel 模型执行完成");
		WxPushUtil.pushSystem1(
				"Seq5=> CODE-MODEL " + tradeDate + " 共[" + codelist.size() + "]条,今日更新条数:" + listHist.size());

	}

	public synchronized String runByCode(String code) {
		int updatedate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		List<CodeBaseModel> listm = new LinkedList<CodeBaseModel>();
		List<CodeBaseModelHist> listh = new LinkedList<CodeBaseModelHist>();
		int oneYearAgo = DateUtil.getPreYear(updatedate);
		int nextYear = DateUtil.getNextYear(updatedate);
//		Map<String, CodeBaseModel> histMap = getALLForMap();
		CodeBaseModel lastOne = getLastOneByCode(code);
		Map<String, CodePool> map = new HashMap<String, CodePool>();
		List<CodePool> list = new LinkedList<CodePool>();
		try {
			StockBaseInfo s = JSON.parseObject(redisUtil.get(code), StockBaseInfo.class);
			getSorce(s, updatedate, oneYearAgo, nextYear, updatedate, listm, listh, false, lastOne, list, map);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, code, "", "");
		}
		return "OK";
	}

	private void getSorce(StockBaseInfo s, int treadeDate, int oneYearAgo, int nextYear, int updatedate,
			List<CodeBaseModel> listLast, List<CodeBaseModelHist> listHist, boolean isJob, CodeBaseModel lastOne,
			List<CodePool> list, Map<String, CodePool> map) {
		String code = s.getCode();
		log.info("Code Model  processing for code:{}", code);
		// 财务
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(code, treadeDate,
				EsQueryPageUtil.queryPage9999);
		// FinanceBaseInfo fbi = financeService.getFinaceReportByLteDate(code,
		// treadeDate);
		if (fbis == null) {
			boolean onlineYear = stockBasicService.online1YearChk(code, treadeDate);
			if (onlineYear) {
				ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, treadeDate + "", "Code Model错误");
			} else {
				log.info("{},Online 上市不足1年", code);
			}
			return;
		}
		FinanceAnalyzer fa = new FinanceAnalyzer();
		for (FinanceBaseInfo fbi : fbis) {
			fa.putJidu1(fbi);
		}
		FinanceBaseInfo fbi = fa.getCurrJidu();
		CodeBaseModel newOne = getLastOneByCode(code);
		newOne.setCode(code);
		newOne.setDate(treadeDate);
		newOne.setCurrYear(fbi.getYear());
		newOne.setCurrQuarter(fbi.getQuarter());
		newOne.setCurrIncomeTbzz(fbi.getYyzsrtbzz());
		newOne.setCurrProfitTbzz(fbi.getGsjlrtbzz());

		// 分红
		BonusHist dh = dividendService.getLastRecordByLteDate(code, oneYearAgo, treadeDate);
		if (dh != null) {
			if (dividendService.SS.equals(dh.getStatus())) {
				newOne.setLastDividendDate(dh.getDividendDate());// 分红年度
			}
		}
		// 回购
		BuyBackInfo bb = buyBackService.getLastRecordByLteDate(code, oneYearAgo, treadeDate);
		if (bb != null) {
			newOne.setLastBackDate(Integer.valueOf(bb.getAnn_date()));// 股东大会通过/完成/停止/实施
		}
		// 质押比例
		ZhiYa zy = zhiYaService.getZhiYa(code);
		newOne.setZyRask(zy.getHasRisk());
		// 限售股解禁
//		ShareFloat sf = shareFloatService.getLastRecordByLteDate(code, treadeDate, nextYear);
//		if (sf != null) {
//			newOne.setFloatDate(sf.getFloatDate());// 解禁日期
//			newOne.setFloatRatio(sf.getFloatRatio());// 流通股份占总股本比率
//		}
//		TODO

		newOne.setForestallYear(0);
		newOne.setForestallQuarter(0);
		newOne.setForestallIncomeTbzz(0);
		newOne.setForestallProfitTbzz(0);

		// 业绩快报(准确的)
		FinYjkb yjkb = financeService.getLastFinaceKbByReportDate(code);
		boolean hasKb = false;

		// 业绩快报(准确的)
		if (yjkb != null) {
			if ((newOne.getCurrYear() == yjkb.getYear() && newOne.getCurrQuarter() < yjkb.getQuarter())// 同一年，季度大于
					|| (yjkb.getYear() > newOne.getCurrYear())) {// 不同年
				newOne.setForestallYear(yjkb.getYear());
				newOne.setForestallQuarter(yjkb.getQuarter());
				newOne.setForestallIncomeTbzz(yjkb.getYyzsrtbzz());
				newOne.setForestallProfitTbzz(yjkb.getJlrtbzz());
				hasKb = true;
			}
		}
		// 业绩预告(类似天气预报,可能不准)
		FinYjyg yjyg = null;
		if (!hasKb) {
			yjyg = financeService.getLastFinaceYgByReportDate(code);
			if (yjyg != null) {
				if ((newOne.getCurrYear() == yjyg.getYear() && newOne.getCurrQuarter() < yjyg.getQuarter())// 同一年,季度大于
						|| (yjyg.getYear() > newOne.getCurrYear())) {// 不同年
					newOne.setForestallYear(yjyg.getYear());
					newOne.setForestallQuarter(yjyg.getQuarter());
					newOne.setForestallProfitTbzz(yjyg.getJlrtbzz());
				}
			}
		}
		processingFinance(list, map, newOne, lastOne, fa, fbis);
		if (bb != null) {
			// 股东大会通过/实施/完成
		} else {
			newOne.setNoBackyear(-1);// 最近1年无回购
		}
		if (dh == null) {
			newOne.setNoDividendyear(-1);// 最近1年无分红
		}

		// 正向分数
		int finals = 0;
		// ======营收同比增长======
		if (fbi.getYyzsrtbzz() > 0) {
			finals += 20;// 营收同比增长
		} else {
			if (fbi.getYyzsrtbzz() > -10) {
				finals += -5;
			} else if (fbi.getYyzsrtbzz() > -20) {
				finals += -10;
			} else {
				finals += -20;
			}
		}

		// ======利润======
		// 归属净利
		if (fbi.getGsjlr() > 0) {
			finals += 5;//
		} else {// 亏损
			finals += -5;
		}
		// 扣非净利
		if (fbi.getKfjlr() > 0) {
			finals += 5;// 营收同比增长
		} else {// 亏损
			finals += -5;
		}
		// 归属净利同比
		if (fbi.getGsjlrtbzz() > 0) {
			finals += 5;//
		} else {// 亏损
			finals += -5;
		}
		// 扣非净利同比
		if (fbi.getKfjlrtbzz() > 0) {
			finals += 5;// 营收同比增长
		} else {// 亏损
			finals += -5;
		}

		// ======资产收益率======
		// 收益率类型:1:自身收益率增长,2: 年收益率超过5.0%*4=20%,4:同时包含12
		if (newOne.getSylType() == 1) {
			finals += 30;
		} else if (newOne.getSylType() == 2) {
			finals += 20;
		} else if (newOne.getSylType() == 4) {
			finals += 40;
		}

		// 分红(3)
		if (newOne.getLastDividendDate() != 0) {
			finals += 3;
		} else {
			finals += -2;
		}
		// 回购(3)
		if (newOne.getLastBackDate() != 0) {
			finals += 3;
		} else {
			finals += -1;
		}
		// 质押
		if (newOne.getZyRask() == 1) {
			finals += -5;
		}
		// 解禁
//		int shareFloat = 0;
//		if (newOne.getFloatDate() > 0) {
//			shareFloat = -3;
//		}
		newOne.setScore(finals);
		newOne.setUdpateDate(updatedate);

		boolean saveHist = true;
		if (lastOne != null) {// 评分变化和季度
			if (lastOne.getScore() == newOne.getScore() && lastOne.getCurrQuarter() == newOne.getCurrQuarter()) {
				saveHist = false;
			} else {
				newOne.setUpScore(finals - lastOne.getScore());
			}
		}
		// 其他
		newOne.setHolderChange(chipsService.holderNumAnalyse(code));

		newOne.setId(code);
		listLast.add(newOne);
		if (saveHist) {
			// copy history
			CodeBaseModelHist hist = new CodeBaseModelHist();
			BeanCopy.copy(newOne, hist);
			hist.setId(code + treadeDate);
			listHist.add(hist);
		}
	}

	private void processingFinance(List<CodePool> list, Map<String, CodePool> map, CodeBaseModel newOne,
			CodeBaseModel lastOne, FinanceAnalyzer fa, List<FinanceBaseInfo> fbis) {

		// log.info(fa.printInfo());
		// 营收(科技类,故事类主要指标)
		newOne.setIncomeUpYear(fa.getCurrYear().getYyzsrtbzz() > 0 ? 1 : 0);// 年报连续营收持续增长
		newOne.setIncomeUpQuarter(fa.getCurrJidu().getYyzsrtbzz() > 0 ? 1 : 0);// 最近季度同比增长
		newOne.setIncomeUp2Quarter(fa.incomeUp2Quarter());// 最近2个季度同比持续增长？

		// 利润(传统行业,销售行业主要指标)
		newOne.setProfitUpYear(fa.getCurrYear().getGsjlrtbzz() > 0 ? 1 : 0);// 归属净利润年报持续增长
		newOne.setProfitUpQuarter(fa.getCurrJidu().getGsjlrtbzz() > 0 ? 1 : 0);// 归属净利润最近季度同比增长
		newOne.setProfitUp2Quarter(fa.profitUp2Quarter());// 最近2个季度同比持续增长？

		// 营收地雷
		newOne.setIncomeDownYear(fa.getCurrYear().getYyzsrtbzz() < 0 ? -1 : 0);// 年营收同比下降
		newOne.setIncomeDownQuarter(fa.getCurrJidu().getYyzsrtbzz() < 0 ? -1 : 0);// 季度营收同比下降
		newOne.setIncomeDown2Quarter(fa.incomeDown2Quarter() == 1 ? -2 : 0);// 最近2个季度同比下降
		// 利润地雷
		newOne.setProfitDownYear(fa.getCurrYear().getGsjlrtbzz() < 0 ? -1 : 0);// 最近年报同比下降TODO//科技类，故事类不看此指标
		newOne.setProfitDownQuarter(fa.getCurrJidu().getGsjlrtbzz() < 0 ? -1 : 0);// 最近季度利润下降TODO//科技类，故事类不看此指标
		newOne.setProfitDown2Quarter(fa.profitDown2Quarter() == 1 ? -2 : 0);// 最近2季度都同比下降
		newOne.setProfitDown2Year(fa.profitDown2Year() == 1 ? -5 : 0);// 年报连续亏损年数？（可能退市）

		// newOne
		evaluateStep1(newOne, fa, fbis);
		CodePool c = findBigBoss(newOne.getCode(), newOne.getDate(), list, map, fbis, fa, lastOne);
		chkZf(c, newOne);// 增发
	}

	// 增发
	private void chkZf(CodePool c, CodeBaseModel newOne) {
		ZengFa zengfa = chipsService.getLastZengFa(newOne.getCode());
		// start 一年以前
		if (zengfa != null && zengfa.getStartDate() > start || zengfa.getEndDate() > start) {
			newOne.setZfStatus(zengfa.getStatus());
		} else {
			newOne.setZfStatus(0);
		}
		c.setZfStatus(newOne.getZfStatus());
	}

	private void evaluateStep1(CodeBaseModel newOne, FinanceAnalyzer fa, List<FinanceBaseInfo> fbis) {
		FinanceBaseInfo currJidu = fa.getCurrJidu();
		newOne.setSyl(currJidu.getJqjzcsyl());
		newOne.setSylttm(plateService.getSylTtm(fbis));
		newOne.setSyldjd(plateService.getSyldjd(currJidu));// 单季度？
		if (newOne.getSyldjd() > newOne.getSylttm()) {
			newOne.setSylType(1);// 自身收益率增长
		}
		if (newOne.getSylttm() >= 5.0) {
			if (newOne.getSylType() == 1) {
				newOne.setSylType(4);// 同时
			} else {
				newOne.setSylType(2);// 年收益率超过5.0%*4=20%
			}
		}
	}

	public List<CodePool> findBigBoss(int treadeDate) {
		log.info("treadeDate={}", treadeDate);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		Map<String, CodePool> map = codePoolService.getCodePoolMap();
		List<CodePool> list = new LinkedList<CodePool>();
		for (StockBaseInfo s : codelist) {
			try {
				List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(s.getCode(), treadeDate,
						EsQueryPageUtil.queryPage9999);
				if (fbis != null && fbis.size() > 0) {
					FinanceAnalyzer fa = new FinanceAnalyzer();
					for (FinanceBaseInfo fbi : fbis) {
						fa.putJidu1(fbi);
					}
					findBigBoss(s.getCode(), treadeDate, list, map, fbis, fa, null);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		return list;
	}

	private CodePool findBigBoss(String code, int treadeDate, List<CodePool> list, Map<String, CodePool> map,
			List<FinanceBaseInfo> fbis, FinanceAnalyzer fa, CodeBaseModel lastOne) {
		log.info("findBigBoss code:{}", code);
		CodePool c = map.get(code);
		if (c == null) {
			c = new CodePool();
			c.setCode(code);
		}
		if (lastOne != null) {
			c.setScore(lastOne.getScore());
		}
		list.add(c);
		c.setUpdateDate(treadeDate);
		// 是否符合中线、1.市盈率和ttm在50以内
		c.setKbygjl(0);
		c.setKbygys(0);
//		if (yjkb != null) {
//			c.setKbygys(yjkb.getYyzsrtbzz());
//			c.setKbygjl(yjkb.getJlrtbzz());
//		} else if (yjyg != null) {
//			c.setKbygjl(yjyg.getJlrtbzz());
//		}

		// 业绩连续
		int continueJidu1 = 0;
		int continueJidu2 = 0;
		boolean cj1 = true;
		int cj2 = 0;
		List<Double> high = new LinkedList<Double>();
		List<Double> high2 = new LinkedList<Double>();
		for (FinanceBaseInfo fbi : fbis) {
			if (cj1 && fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 连续季度增长
				continueJidu1++;
				high.add(fbi.getYyzsrtbzz());
			} else {
				cj1 = false;
			}
			if (cj2 <= 1 && fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 允许一次断连续
				continueJidu2++;
				high2.add(fbi.getYyzsrtbzz());
			} else {
				cj2++;
			}
		}
		boolean isok = false;
		if (continueJidu1 > 3 || continueJidu2 > 5) {
			if (continueJidu1 > 3) {
				int cn = 0;
				for (Double h : high) {// 连续超过25%的次数超过一半
					if (h > 25.0) {
						cn++;
					}
				}
				if (cn * 2 > continueJidu1) {
					isok = true;
				}
			} else if (continueJidu2 > 5) {
				int cn = 0;
				for (Double h : high2) {
					if (h > 25.0) {
						cn++;
					}
				}
				if (cn * 2 > continueJidu2) {
					isok = true;
				}
			}
		}
		c.setIsok(isok);
		c.setContinYj1(continueJidu1);
		c.setContinYj2(continueJidu2);
		return c;
	}

	public CodeBaseModel getLastOneByCode(String code) {
		log.info("getLastOneByCode:{}", code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModel> page = codeBaseModelDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		CodeBaseModel cbm = new CodeBaseModel();
		cbm.setCode(code);
		return cbm;
	}

	public Map<String, CodeBaseModel> getALLForMap() {
		List<CodeBaseModel> list = getALLForList();
		Map<String, CodeBaseModel> map = new HashMap<String, CodeBaseModel>();
		for (CodeBaseModel c : list) {
			map.put(c.getCode(), c);
		}
		return map;
	}

	private List<CodeBaseModel> getALLForList() {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<CodeBaseModel> page = codeBaseModelDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	// 重设分值为0为重跑
	public void reset() {
		List<CodeBaseModel> list = getALLForList();
		for (CodeBaseModel c : list) {
			c.setScore(0);
		}
		codeBaseModelDao.saveAll(list);
	}

	public List<CodeBaseModelHist> getListByCode(String code, EsQueryPageReq querypage) {
		log.info("getListByCode:{}", code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModelHists");
		return null;
	}

	public CodeBaseModelHist getHistOneById(String id) {
		log.info("getHistOneById:{}", id);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	public List<CodeBaseModel> getList(String code, int orderBy, String aliasCode, String conceptName, int asc,
			EsQueryPageReq querypage, String zfStatus) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else if (StringUtils.isNotBlank(aliasCode)) {
			List<String> list = conceptService.listCodesByAliasCode(aliasCode);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		} else if (StringUtils.isNotBlank(conceptName)) {
			List<String> list = listCodeByCodeConceptName(conceptName);
			if (list.size() > 0) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		}
		if (StringUtils.isNotBlank(zfStatus)) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfStatus", Integer.valueOf(zfStatus)));
		}
		String field = "score";
		if (orderBy == 2) {
			field = "upScore";
		} else if (orderBy == 2) {
			field = "upScore";
		} else if (orderBy == 3) {
			field = "sylttm";
		} else if (orderBy == 4) {
			field = "syl";
		} else if (orderBy == 5) {
			field = "sylType";
		}
//		<option value="1" selected="selected">综合评分</option>
//		<option value="2">评分变化</option>
//		<option value="3">资产收益率ttm</option>
//		<option value="4">资产收益率报告期</option>
//		<option value="5">资产收益评级</option>
		SortOrder order = SortOrder.DESC;
		if (asc == 2) {
			order = SortOrder.ASC;
		}

		FieldSortBuilder sort = SortBuilders.fieldSort(field).unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodeBaseModel> page = codeBaseModelDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	public List<CodeBaseModelResp> getListForWeb(String code, int orderBy, String conceptId, String conceptName,
			int asc, EsQueryPageReq querypage, String zfStatus) {
		log.info(
				"CodeBaseModel getListForWeb code={},orderBy={},asc={},num={},size={},conceptId={},conceptName={},zfStatus={}",
				code, orderBy, asc, querypage.getPageNum(), querypage.getPageSize(), conceptId, conceptName, zfStatus);

		List<CodeBaseModel> list = getList(code, orderBy, conceptId, conceptName, asc, querypage, zfStatus);
		List<CodeBaseModelResp> res = new LinkedList<CodeBaseModelResp>();
		if (list != null) {
			for (CodeBaseModel dh : list) {
				CodeBaseModelResp resp = new CodeBaseModelResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setIncomeShow(dh.getCurrIncomeTbzz() + "%");
				if (dh.getForestallIncomeTbzz() > 0) {
					resp.setIncomeShow(resp.getIncomeShow() + "(" + dh.getForestallIncomeTbzz() + "%)");
				}
				resp.setProfitShow(dh.getCurrProfitTbzz() + "%");
				if (dh.getForestallIncomeTbzz() > 0) {
					resp.setProfitShow(resp.getProfitShow() + "(" + dh.getForestallProfitTbzz() + "%)");
				}
				if (resp.getFloatRatio() > 0) {
					resp.setFloatRatio(CurrencyUitl.roundHalfUp(resp.getFloatRatio()));
				}
				res.add(resp);
			}
		}
		return res;
	}

	public List<String> listCodeByCodeConceptName(String conceptName) {
		List<String> codes = new LinkedList<String>();
		List<StockBaseInfo> l = stockBasicService.getAllOnStatusList();
		conceptName = conceptName.trim();
		for (StockBaseInfo s : l) {
			if (s.getThsIndustry().contains(conceptName)) {
				codes.add(s.getCode());
			}
		}
		return codes;
	}

}
