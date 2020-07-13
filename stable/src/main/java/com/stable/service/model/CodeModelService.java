package com.stable.service.model;

import java.util.Date;
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

import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsCodeBaseModelDao;
import com.stable.service.BuyBackService;
import com.stable.service.ConceptService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.DividendService;
import com.stable.service.FinanceService;
import com.stable.service.PledgeStatService;
import com.stable.service.ShareFloatService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.RedisUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.DividendHistory;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.PledgeStat;
import com.stable.vo.bus.ShareFloat;
import com.stable.vo.bus.StockBaseInfo;
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
	private AvgService avgService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private EsCodeBaseModelDao codeBaseModelDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private ShareFloatService shareFloatService;

	private final EsQueryPageReq queryPage8 = new EsQueryPageReq(8);
	private final EsQueryPageReq deleteQueryPage = new EsQueryPageReq(9999);

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
					log.info("processing date={}", redisDate);
					run(isJob, redisDate);
					// 缓存已经处理的日期
					redisUtil.set(RedisConstant.RDS_MODEL_V1_DATE, redisDate);
					// 新增一天
					Date d1 = DateUtil.addDate(redisDate + "", 1);
					redisDate = Integer.valueOf(DateUtil.formatYYYYMMDD(d1));
					if (redisDate > today) {
						log.info("today:{},date:{} 循环结束", today, redisDate);
						break;
					}
				}
			} else {// 手动某一天
				log.info("processing date={}", today);
				List<CodeBaseModel> deleteall = getListByCode(today, deleteQueryPage);
				if (deleteall != null) {
					log.info("删除当天{}记录条数{}", today, deleteall.size());
					codeBaseModelDao.deleteAll(deleteall);
					Thread.sleep(3 * 1000);
				}
				log.info("模型date={}开始", today);
				run(isJob, today);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "模型运行异常", "", "");
			WxPushUtil.pushSystem1("模型运行异常..");
		}
	}

	private void run(boolean isJob, int treadeDate) {
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : codelist) {
			String code = s.getCode();
			log.info("Code Model  processing for code:{}", code);
			if (!stockBasicService.online1Year(code)) {
				log.info("{},Online 上市不足1年", code);
				continue;
			}
			// TODO 公告日期比较及时，但是可能会不通过，需要看分红是否实施，回购实际金额等
			CodeBaseModel lastOne = getLastModelByCode(code, treadeDate);
			// 财务
			FinanceBaseInfo fbi = financeService.getFinaceReportByLteDate(code, treadeDate);
			if (fbi == null) {
				ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, treadeDate + "", "Code Model错误");
				continue;
			}
			CodeBaseModel newOne = new CodeBaseModel();
			newOne.setCode(code);
			newOne.setDate(treadeDate);
			newOne.setCurrYear(fbi.getYear());
			newOne.setCurrQuarter(fbi.getQuarter());
			// 分红
			DividendHistory dh = dividendService.getLastRecordByLteDate(code, treadeDate);
			if (dh != null) {
				newOne.setLastDividendDate(dh.getEnd_date());// 分红年度
			}
			// 回购
			BuyBackInfo bb = buyBackService.getLastRecordByLteDate(code, treadeDate);
			if (bb != null) {
				newOne.setLastBackDate(Integer.valueOf(bb.getAnn_date()));// 股东大会通过
			}
			// 质押比例
			PledgeStat ps = pledgeStatService.getLastRecords(code, treadeDate);
			if (ps != null) {
				newOne.setEndDate(ps.getEndDate());// 截止日期
				newOne.setPledgeRatio(ps.getPledgeRatio());// 质押比例
			}
			// 限售股解禁
			ShareFloat sf = shareFloatService.getLastRecordByLteDate(code, treadeDate);
			if (sf != null) {
				newOne.setFloatDate(sf.getAnnDate());// 解禁日期
				newOne.setFloatRatio(sf.getFloatRatio());// 流通股份占总股本比率
			}
			log.info("{},KeyString:{}", code, newOne.getKeyString());
			if (lastOne != null && lastOne.getKeyString().equals(newOne.getKeyString())) {
				log.info("{},lastOne equals newOne!");
				continue;
			}

		}
	}

	private void processingFinance(CodeBaseModel base) {
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(base.getCode(), base.getDate(),
				queryPage8);

		int incomeUpYears = 0;
		
		for (FinanceBaseInfo fbi : fbis) {
			if(fbi.getQuarter()==4 ) {
				
			}
		}

		// 营收(科技类,故事类主要指标)
//		private ;// 年报连续营年数？
//		private int incomeUp2yearc;// 年报连续2年营收持续增长？
//		private int incomeUpQuartert;// 最近季度同比增长？
//		private int incomeUp2quarterc;// 最近2个季度同比持续增长？
//		// 利润(传统行业,销售行业主要指标)
//		private int profitUpYears;// 年报盈利年数？
//		private int profitUp2yearc;// 年报连续2年盈利持续增长？
//		private int profitUpQuartert;// 最近季度同比增长？
//		private int profitUp2quarterc;// 最近2个季度同比持续增长？
	}

	public CodeBaseModel getLastModelByCode(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

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

}
