package com.stable.service.model;

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

import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsCodeBaseModelDao;
import com.stable.es.dao.base.EsCodeBaseModelHistDao;
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
import com.stable.vo.bus.DividendHistory;
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
						run(isJob, redisDate);
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
				run(isJob, today);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "CodeModel模型运行异常", "", "");
			WxPushUtil.pushSystem1("CodeModel模型运行异常..");
		}
	}

	private synchronized void run(boolean isJob, int treadeDate) {
		int updatedate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		List<CodeBaseModel> listm = new LinkedList<CodeBaseModel>();
		List<CodeBaseModelHist> listh = new LinkedList<CodeBaseModelHist>();
		int oneYearAgo = DateUtil.getPreYear(treadeDate);
		int nextYear = DateUtil.getNextYear(treadeDate);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : codelist) {
			try {
				String code = s.getCode();
				log.info("Code Model  processing for code:{}", code);
				CodeBaseModel lastOne = getLastModelByCode(code);
				// 财务
				FinanceBaseInfo fbi = financeService.getFinaceReportByLteDate(code, treadeDate);
				if (fbi == null) {
					boolean onlineYear = stockBasicService.online1Year(code);
					if (onlineYear) {
						ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, treadeDate + "",
								"Code Model错误");
					} else {
						log.info("{},Online 上市不足1年", code);
					}
					continue;
				}
				CodeBaseModel newOne = new CodeBaseModel();
				newOne.setCode(code);
				newOne.setDate(treadeDate);
				newOne.setCurrYear(fbi.getYear());
				newOne.setCurrQuarter(fbi.getQuarter());
				// 分红
				DividendHistory dh = dividendService.getLastRecordByLteDate(code, oneYearAgo, treadeDate);
				if (dh != null) {
					newOne.setLastDividendDate(dh.getEnd_date());// 分红年度
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
					newOne.setFloatDate(sf.getAnnDate());// 解禁日期
					newOne.setFloatRatio(sf.getFloatRatio());// 流通股份占总股本比率
				}
				log.info("{},KeyString:{}", code, newOne.getKeyString());
				if (lastOne != null && lastOne.getKeyString().equals(newOne.getKeyString())) {
					log.info("{},lastOne equals newOne!");
					continue;
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
				int incomeupbase = newOne.getIncomeUpYear() + newOne.getIncomeUpQuarter()
						+ newOne.getIncomeUp2Quarter();
				incomeupbase += incomeupbase * 20;
				// 利润(5)
				int profitupbase = newOne.getProfitUpYear() + newOne.getProfitUpQuarter()
						+ newOne.getProfitUp2Quarter();
				profitupbase += profitupbase * 5;
				// 分红(5)
				int dividendbase = 0;
				if (newOne.getLastDividendDate() != 0) {
					dividendbase = 3;
				} else {
					dividendbase = -2;
				}
				// 回购(5)
				int backbase = 0;
				if (newOne.getLastBackDate() != 0) {
					backbase = 3;
				} else {
					backbase = -1;
				}
				//
				int up = incomeupbase + profitupbase + dividendbase + backbase;// 分
				// 负向分数
				// 营收(10)
				int incomedownbase = newOne.getIncomeDownYear() + newOne.getIncomeDownQuarter()
						+ newOne.getIncomeDown2Quarter();
				incomedownbase += incomedownbase * -10;
				// 利润(3)
				int profitdownbase = newOne.getProfitDownYear() + newOne.getProfitDownQuarter()
						+ newOne.getProfitDown2Quarter() + newOne.getProfitDown2Year();
				profitdownbase += profitdownbase * -3;
				// 质押
				int pledgeRatio = 0;
				if (newOne.getPledgeRatio() > 90) {
					pledgeRatio = -5;
				}
				// 解禁
				int shareFloat = 0;
				if (newOne.getFloatDate() > 0) {
					backbase = -3;
				}
				int down = incomedownbase + profitdownbase + pledgeRatio + shareFloat;
				int finals = up + down;

				newOne.setScore(finals);
				newOne.setUdpateDate(updatedate);
				if (lastOne != null) {// 评分上涨
					newOne.setUpScore(finals - lastOne.getScore());
				}
				// copy history
				CodeBaseModelHist hist = new CodeBaseModelHist();
				BeanCopy.copy(newOne, hist);
				newOne.setId(code);
				hist.setId(code + treadeDate);
				listm.add(newOne);
				listh.add(hist);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		if (listm.size() > 0) {
			codeBaseModelDao.saveAll(listm);
			codeBaseModelHistDao.saveAll(listh);
		}
		log.info("Code 模型执行完成");
		WxPushUtil.pushSystem1("Seq5=> CODE-MODEL 共[" + codelist.size() + "]条,今日更新条数:" + listm.size());
	}

	private void processingFinance(CodeBaseModel base) {
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(base.getCode(), base.getDate(),
				queryPage8);

		FinanceAnalyzer fa = new FinanceAnalyzer();

		for (FinanceBaseInfo fbi : fbis) {
			fa.putJidu1(fbi);
		}
		// 营收(科技类,故事类主要指标)
		base.setIncomeUpYear(fa.getCurrYear().getYyzsrtbzz() > 0 ? 1 : 0);// 年报连续营收持续增长？
		base.setIncomeUpQuarter(fa.getCurrJidu().getYyzsrtbzz() > 0 ? 1 : 0);// 最近季度同比增长？
		base.setIncomeUp2Quarter(fa.getincome2Jiduc());// 最近2个季度同比持续增长？

		// 利润(传统行业,销售行业主要指标)
		base.setProfitUpYear(fa.getCurrYear().getGsjlrtbzz() > 0 ? 1 : 0);// 归属净利润年报持续增长？
		base.setProfitUpQuarter(fa.getCurrJidu().getGsjlrtbzz() > 0 ? 1 : 0);// 归属净利润最近季度同比增长？
		base.setProfitUp2Quarter(fa.profitUp2quarter());// 最近2个季度同比持续增长？

		// 营收地雷
		base.setIncomeDownYear(fa.getCurrYear().getYyzsrtbzz() < 0 ? -1 : 0);// 年营收同比下降
		base.setIncomeDownQuarter(fa.getCurrJidu().getYyzsrtbzz() < 0 ? -1 : 0);// 季度营收同比下降
		base.setIncomeDown2Quarter(fa.incomeDown2Quarter() == 1 ? -2 : 0);// 最近2个季度同比下降
		// 利润地雷
		base.setProfitDownQuarter(fa.getCurrJidu().getGsjlr() < 0 ? -1 : 0);// 最近季度利润下降TODO//科技类，故事类不看此指标
		base.setProfitDown2Quarter(fa.profitDown2Quarter() == 1 ? -2 : 0);// 最近2季度都同比下降？
		base.setProfitDownYear(fa.getCurrYear().getGsjlrtbzz() < 0 ? -1 : 0);// 最近年报同比下降TODO//科技类，故事类不看此指标
		base.setProfitDown2Year(fa.profitDown2Year() == 1 ? -5 : 0);// 年报连续亏损年数？（可能退市）
	}

	public CodeBaseModel getLastModelByCode(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<CodeBaseModel> page = codeBaseModelDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no last records CodeBaseModels");
		return null;

	}

	public List<CodeBaseModel> getListByCode(int date, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

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

	public List<CodeBaseModel> getList(String code, int orderBy, int asc, EsQueryPageReq querypage) {
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

	public List<CodeBaseModelResp> getListForWeb(String code, int orderBy, int asc, EsQueryPageReq querypage) {
		log.info("CodeBaseModel getListForWeb code={},orderBy={},asc={},num={},size={}", code, orderBy, asc,
				querypage.getPageNum(), querypage.getPageSize());
		List<CodeBaseModel> list = getList(code, orderBy, asc, querypage);
		List<CodeBaseModelResp> res = new LinkedList<CodeBaseModelResp>();
		if (list != null) {
			for (CodeBaseModel dh : list) {
				CodeBaseModelResp resp = new CodeBaseModelResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}
}
