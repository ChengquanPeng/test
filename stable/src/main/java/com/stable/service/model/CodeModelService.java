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
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsCodeBaseModelDao;
import com.stable.es.dao.base.EsCodeBaseModelHistDao;
import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.service.BuyBackService;
import com.stable.service.DividendService;
import com.stable.service.FinanceService;
import com.stable.service.PledgeStatService;
import com.stable.service.ShareFloatService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.FinanceAnalyzer;
import com.stable.utils.BeanCopy;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.RedisUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.DividendHistory;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.PledgeStat;
import com.stable.vo.bus.ShareFloat;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelService {

	@Autowired
	private DividendService dividendService;
	@Autowired
	private BuyBackService buyBackService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private PledgeStatService pledgeStatService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsCodeBaseModelDao codeBaseModelDao;
	@Autowired
	private EsCodeBaseModelHistDao codeBaseModelHistDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private ShareFloatService shareFloatService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private EsConceptDao esConceptDao;
	@Autowired
	private EsCodeConceptDao esCodeConceptDao;

	private final EsQueryPageReq queryPage8 = new EsQueryPageReq(8);

	public synchronized void runJob(boolean isJob, int today) {
		try {
			if (isJob) {
				int redisDate = 0;
				String strDate = redisUtil.get(RedisConstant.RDS_MODEL_BASE_DATE);
				if (StringUtils.isBlank(strDate)) {// 无缓存，从当天开始
					redisDate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
				} else {// 缓存的日期是已经执行过，需要+1天
					Date d = DateUtil.addDate(strDate, 1);
					redisDate = Integer.valueOf(DateUtil.formatYYYYMMDD(d));
				}
				while (true) {
					log.info("CodeModel processing date={}", redisDate);
					if (tradeCalService.isOpen(redisDate)) {
						log.info("processing date={}", redisDate);
						run(redisDate);
					} else {
						log.info("{}非交易日", redisDate);
					}
					// 缓存已经处理的日期
					redisUtil.set(RedisConstant.RDS_MODEL_BASE_DATE, redisDate);
					// 新增一天
					Date d1 = DateUtil.addDate(redisDate + "", 1);
					redisDate = Integer.valueOf(DateUtil.formatYYYYMMDD(d1));
					if (redisDate > today) {
						log.info("CodeModel today:{},date:{} 循环结束", today, redisDate);
						break;
					}
				}
			} else {// 手动某一天
				log.info("CodeModel processing date={}", today);
				if (!tradeCalService.isOpen(today)) {
					log.info("{}非交易日", today);
					return;
				}
				run(today);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "CodeModel模型运行异常", "", "");
			WxPushUtil.pushSystem1("CodeModel模型运行异常..");
		}
	}

	private synchronized void run(int treadeDate) {
		int updatedate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		List<CodeBaseModel> listLast = new LinkedList<CodeBaseModel>();
		List<CodeBaseModelHist> listHist = new LinkedList<CodeBaseModelHist>();
		int oneYearAgo = DateUtil.getPreYear(treadeDate);
		int nextYear = DateUtil.getNextYear(treadeDate);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		Map<String, CodeBaseModel> histMap = getALLForMap();
		for (StockBaseInfo s : codelist) {
			try {
				getSorce(s, treadeDate, oneYearAgo, nextYear, updatedate, listLast, listHist, true, null,
						histMap.get(s.getCode()));
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		if (listLast.size() > 0) {
			codeBaseModelDao.saveAll(listLast);
		}
		if (listHist.size() > 0) {
			codeBaseModelHistDao.saveAll(listHist);
		}
		log.info("CodeModel 模型执行完成");
		WxPushUtil.pushSystem1(
				"Seq5=> CODE-MODEL " + treadeDate + " 共[" + codelist.size() + "]条,今日更新条数:" + listHist.size());
	}

	public synchronized String run(String code) {
		int updatedate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		List<CodeBaseModel> listm = new LinkedList<CodeBaseModel>();
		List<CodeBaseModelHist> listh = new LinkedList<CodeBaseModelHist>();
		int oneYearAgo = DateUtil.getPreYear(updatedate);
		int nextYear = DateUtil.getNextYear(updatedate);
		StringBuffer res = new StringBuffer();
//		Map<String, CodeBaseModel> histMap = getALLForMap();
		CodeBaseModel lastOne = getLastOneByCode(code);
		try {
			StockBaseInfo s = JSON.parseObject(redisUtil.get(code), StockBaseInfo.class);
			getSorce(s, updatedate, oneYearAgo, nextYear, updatedate, listm, listh, false, res, lastOne);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "", "", "");
		}
		return res.toString();
	}

	private void getSorce(StockBaseInfo s, int treadeDate, int oneYearAgo, int nextYear, int updatedate,
			List<CodeBaseModel> listLast, List<CodeBaseModelHist> listHist, boolean isJob, StringBuffer sb,
			CodeBaseModel lastOne) {
		String code = s.getCode();
		log.info("Code Model  processing for code:{}", code);
		// 财务
		FinanceBaseInfo fbi = financeService.getFinaceReportByLteDate(code, treadeDate);
		if (fbi == null) {
			boolean onlineYear = stockBasicService.online1Year(code, treadeDate);
			if (onlineYear) {
				ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, treadeDate + "", "Code Model错误");
			} else {
				log.info("{},Online 上市不足1年", code);
			}
			return;
		}
		CodeBaseModel newOne = new CodeBaseModel();
		newOne.setCode(code);
		newOne.setDate(treadeDate);
		newOne.setCurrYear(fbi.getYear());
		newOne.setCurrQuarter(fbi.getQuarter());
		newOne.setCurrIncomeTbzz(fbi.getYyzsrtbzz());
		newOne.setCurrProfitTbzz(fbi.getGsjlrtbzz());

		// 分红
		DividendHistory dh = dividendService.getLastRecordByLteDate(code, oneYearAgo, treadeDate);
		if (dh != null) {
			if (dividendService.SS.equals(dh.getDiv_proc())) {
				newOne.setLastDividendDate(dh.getEnd_date());// 分红年度
			} else {
				// 股东大会通过:不分配。。。
				if (dh.getCash_div() > 0) {
					newOne.setLastDividendDate(dh.getEnd_date());
				} else {
					dh = null;
				}
			}
		}
		// 回购
		BuyBackInfo bb = buyBackService.getLastRecordByLteDate(code, oneYearAgo, treadeDate);
		if (bb != null) {
			newOne.setLastBackDate(Integer.valueOf(bb.getAnn_date()));// 股东大会通过/完成/停止/实施
		}
		// 质押比例
		PledgeStat ps = pledgeStatService.getLastRecords(code, treadeDate);
		if (ps != null) {
			newOne.setEndDate(ps.getEndDate());// 截止日期
			newOne.setPledgeRatio(ps.getPledgeRatio());// 质押比例
		}
		// 限售股解禁
		ShareFloat sf = shareFloatService.getLastRecordByLteDate(code, treadeDate, nextYear);
		if (sf != null) {
			newOne.setFloatDate(sf.getFloatDate());// 解禁日期
			newOne.setFloatRatio(sf.getFloatRatio());// 流通股份占总股本比率
		}
		// 业绩快报(准确的)
		FinYjkb yjkb = financeService.getLastFinaceKbByReportDate(code);
		boolean hasKb = false;

		newOne.setForestallYear(0);
		newOne.setForestallQuarter(0);
		newOne.setForestallIncomeTbzz(0);
		newOne.setForestallProfitTbzz(0);

		double ystbzz = fbi.getYyzsrtbzz();
		double jltbzz = fbi.getGsjlrtbzz();

		// 业绩快报(准确的)
		if (yjkb != null) {
			if ((newOne.getCurrYear() == yjkb.getYear() && newOne.getCurrQuarter() < yjkb.getQuarter())// 同一年，季度大于
					|| (yjkb.getYear() > newOne.getCurrYear())) {// 不同年
				newOne.setForestallYear(yjkb.getYear());
				newOne.setForestallQuarter(yjkb.getQuarter());
				newOne.setForestallIncomeTbzz(yjkb.getYyzsrtbzz());
				newOne.setForestallProfitTbzz(yjkb.getJlrtbzz());
				hasKb = true;
				if (yjkb.getYyzsrtbzz() != 0) {// 可能出现0的数据,则按财报进行计算
					ystbzz = yjkb.getYyzsrtbzz();
				}
				if (yjkb.getJlrtbzz() != 0) {// 可能出现0的数据,则按财报进行计算
					jltbzz = yjkb.getJlrtbzz();
				}
			}
		}
		// 业绩预告(类似天气预报,可能不准)
		if (!hasKb) {
			FinYjyg yjyg = financeService.getLastFinaceYgByReportDate(code);
			if (yjyg != null) {
				if ((newOne.getCurrYear() == yjyg.getYear() && newOne.getCurrQuarter() < yjyg.getQuarter())// 同一年,季度大于
						|| (yjyg.getYear() > newOne.getCurrYear())) {// 不同年
					newOne.setForestallYear(yjyg.getYear());
					newOne.setForestallQuarter(yjyg.getQuarter());
					newOne.setForestallProfitTbzz(yjyg.getJlrtbzz());
					if (yjyg.getJlrtbzz() != 0) {// 可能出现0的数据,则按财报进行计算
						jltbzz = yjyg.getJlrtbzz();
					}
				}
			}
		}

		processingFinance(newOne);
		if (bb != null) {
			// 股东大会通过/实施/完成
		} else {
			newOne.setNoBackyear(-1);// 最近1年无回购
		}
		if (dh == null) {
			newOne.setNoDividendyear(-1);// 最近1年无分红
		}

		// 正向分数
		// 营收(20)
		int incomeupbase = newOne.getIncomeUpYear() + newOne.getIncomeUpQuarter() + newOne.getIncomeUp2Quarter();
		// 利润(5)
		int profitupbase = newOne.getProfitUpYear() + newOne.getProfitUpQuarter() + newOne.getProfitUp2Quarter();

		// 分红(3)
		int dividendbase = 0;
		if (newOne.getLastDividendDate() != 0) {
			dividendbase = 3;
		} else {
			dividendbase = -2;
		}
		// 回购(3)
		int backbase = 0;
		if (newOne.getLastBackDate() != 0) {
			backbase = 3;
		} else {
			backbase = -1;
		}

		// 负向分数
		// 营收(10)
		int incomedownbase = newOne.getIncomeDownYear() + newOne.getIncomeDownQuarter()
				+ newOne.getIncomeDown2Quarter();

		// 利润(3)
		int profitdownbase = newOne.getProfitDownYear() + newOne.getProfitDownQuarter() + newOne.getProfitDown2Quarter()
				+ newOne.getProfitDown2Year();

		// 质押
		int pledgeRatio = 0;
		if (newOne.getPledgeRatio() > 60) {
			pledgeRatio = -5;
		}
		// 解禁
		int shareFloat = 0;
		if (newOne.getFloatDate() > 0) {
			shareFloat = -3;
		}

		// 权重
		incomeupbase += incomeupbase * 20;
		profitupbase += profitupbase * 5;

		incomedownbase += incomedownbase * -10;
		profitdownbase += profitdownbase * -3;

		// 营收-业绩-按比例加减分
		int yjsorce = 0;
		if (ystbzz != 0) {
			if (ystbzz > 0) {
				if (ystbzz > 100) {
					yjsorce += 10;
				} else if (ystbzz > 40) {
					yjsorce += 5;
				}
			} else {
				if (ystbzz < -100) {
					yjsorce += -10;
				} else if (ystbzz < -30) {
					yjsorce += -5;
				}
			}
		}
		if (jltbzz != 0) {
			if (jltbzz > 0) {
				if (jltbzz > 100) {
					yjsorce += 6;
				} else if (jltbzz > 40) {
					yjsorce += 3;
				}
			} else {
				if (jltbzz < -100) {
					yjsorce += -5;
				} else if (jltbzz < -30) {
					yjsorce += -3;
				}
			}
		}

		int up = incomeupbase + profitupbase + dividendbase + backbase;
		int down = incomedownbase + profitdownbase + pledgeRatio + shareFloat;
		int finals = up + down + yjsorce;

		if (sb != null) {
			sb.append("报告期:incomeupbase[" + incomeupbase + "(x20)] = IncomeUpYear[" + newOne.getIncomeUpYear()
					+ "] + IncomeUpQuarter[" + newOne.getIncomeUpQuarter() + "] + IncomeUp2Quarter:["
					+ newOne.getIncomeUp2Quarter() + "]");
			sb.append("报告期:profitupbase[" + profitupbase + "(x5)] = ProfitUpYear[" + newOne.getProfitUpYear()
					+ "] + ProfitUpQuarter[" + newOne.getProfitUpQuarter() + "] + ProfitUp2Quarter["
					+ newOne.getProfitUp2Quarter() + "]");
			sb.append("dividendbase = " + dividendbase);
			sb.append("backbase = " + backbase);
			sb.append("报告期:incomedownbase[" + incomedownbase + "(x-10)] = IncomeDownYear[" + newOne.getIncomeDownYear()
					+ "] + IncomeDownQuarter[" + newOne.getIncomeDownQuarter() + "] + IncomeDown2Quarter["
					+ newOne.getIncomeDown2Quarter() + "]");
			sb.append("profitdownbase[" + profitdownbase + "(x-3)] = ProfitDownYear[" + newOne.getProfitDownYear()
					+ "] + ProfitDownQuarter[" + newOne.getProfitDownQuarter() + "] + ProfitDown2Quarter["
					+ newOne.getProfitDown2Quarter() + "]");
			sb.append("pledgeRatio = " + pledgeRatio);
			sb.append("shareFloat = " + shareFloat);
			sb.append("up = " + up);
			sb.append("down = " + down);
			sb.append("业绩比例:yjsorce = " + yjsorce);
			sb.append("最后分finals  = " + finals);
		}

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

	private void processingFinance(CodeBaseModel base) {
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(base.getCode(), base.getDate(),
				queryPage8);

		FinanceAnalyzer fa = new FinanceAnalyzer();

		for (FinanceBaseInfo fbi : fbis) {
			fa.putJidu1(fbi);
		}
		// log.info(fa.printInfo());
		// 营收(科技类,故事类主要指标)
		base.setIncomeUpYear(fa.getCurrYear().getYyzsrtbzz() > 0 ? 1 : 0);// 年报连续营收持续增长
		base.setIncomeUpQuarter(fa.getCurrJidu().getYyzsrtbzz() > 0 ? 1 : 0);// 最近季度同比增长
		base.setIncomeUp2Quarter(fa.incomeUp2Quarter());// 最近2个季度同比持续增长？

		// 利润(传统行业,销售行业主要指标)
		base.setProfitUpYear(fa.getCurrYear().getGsjlrtbzz() > 0 ? 1 : 0);// 归属净利润年报持续增长
		base.setProfitUpQuarter(fa.getCurrJidu().getGsjlrtbzz() > 0 ? 1 : 0);// 归属净利润最近季度同比增长
		base.setProfitUp2Quarter(fa.profitUp2Quarter());// 最近2个季度同比持续增长？

		// 营收地雷
		base.setIncomeDownYear(fa.getCurrYear().getYyzsrtbzz() < 0 ? -1 : 0);// 年营收同比下降
		base.setIncomeDownQuarter(fa.getCurrJidu().getYyzsrtbzz() < 0 ? -1 : 0);// 季度营收同比下降
		base.setIncomeDown2Quarter(fa.incomeDown2Quarter() == 1 ? -2 : 0);// 最近2个季度同比下降
		// 利润地雷
		base.setProfitDownYear(fa.getCurrYear().getGsjlrtbzz() < 0 ? -1 : 0);// 最近年报同比下降TODO//科技类，故事类不看此指标
		base.setProfitDownQuarter(fa.getCurrJidu().getGsjlrtbzz() < 0 ? -1 : 0);// 最近季度利润下降TODO//科技类，故事类不看此指标
		base.setProfitDown2Quarter(fa.profitDown2Quarter() == 1 ? -2 : 0);// 最近2季度都同比下降
		base.setProfitDown2Year(fa.profitDown2Year() == 1 ? -5 : 0);// 年报连续亏损年数？（可能退市）
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
		return null;
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
		EsQueryPageReq querypage = new EsQueryPageReq(9999);
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

	public List<CodeBaseModel> getList(String code, int orderBy, String conceptId, int asc, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		String field = "score";
		if (orderBy == 2) {
			field = "upScore";
		}
		SortOrder order = SortOrder.DESC;
		if (asc == 2) {
			order = SortOrder.ASC;
		}
		if (StringUtils.isNotBlank(conceptId)) {
			List<String> list = listCodeByCodeConceptId(conceptId);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
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

	public List<CodeBaseModelResp> getListForWeb(String code, int orderBy, String conceptId, int asc,
			EsQueryPageReq querypage) {
		log.info("CodeBaseModel getListForWeb code={},orderBy={},asc={},num={},size={},conceptId={}", code, orderBy,
				asc, querypage.getPageNum(), querypage.getPageSize(), conceptId);

		List<CodeBaseModel> list = getList(code, orderBy, conceptId, asc, querypage);
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
				res.add(resp);
			}
		}
		return res;
	}

	private String getConceptId(String conceptId) {
		EsQueryPageReq querypage = new EsQueryPageReq(1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(conceptId)) {
			bqb.must(QueryBuilders.matchPhraseQuery("aliasCode", conceptId));
		} else {
			return null;
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<Concept> page = esConceptDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0).getId();
		}
		log.info("no records aliasCode:{}", conceptId);
		return null;

	}

	private List<String> listCodeByCodeConceptId(String conceptId) {
		EsQueryPageReq querypage = new EsQueryPageReq(1000);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		conceptId = getConceptId(conceptId);
		if (StringUtils.isNotBlank(conceptId)) {
			bqb.must(QueryBuilders.matchPhraseQuery("conceptId", conceptId));
		} else {
			return null;
		}

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<CodeConcept> page = esCodeConceptDao.search(sq);
		if (page != null && !page.isEmpty()) {
			List<CodeConcept> list = page.getContent();
			List<String> codes = new LinkedList<String>();
			for (CodeConcept cc : list) {
				codes.add(cc.getCode());
			}
			return codes;
		}
		log.info("no records listCodeByCodeConceptId:{}", conceptId);
		return null;
	}
}
