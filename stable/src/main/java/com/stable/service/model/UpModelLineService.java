package com.stable.service.model;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsModelV1Dao;
import com.stable.service.ConceptService;
import com.stable.service.ConceptService.ConceptInfo;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LineTickData;
import com.stable.service.model.data.LineVol;
import com.stable.service.model.data.StrongService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.MyRunnable;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker2nd;
import com.stable.utils.WxPushUtil;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.PriceLife;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UpModelLineService {

	@Autowired
	private StrongService strongService;
	@Autowired
	private TickDataService tickDataService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private EsModelV1Dao esModelV1Dao;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private ConceptService conceptService;

	private final EsQueryPageReq queryPage = new EsQueryPageReq(250);
	private final EsQueryPageReq deleteQueryPage = new EsQueryPageReq(9999);

	public synchronized void runJob(int date) {
		try {
			if (date == 0) {
				int today = Integer.valueOf(DateUtil.formatYYYYMMDD(new Date()));
				String strDate = redisUtil.get(RedisConstant.RDS_MODEL_V1_DATE);
				if (StringUtils.isBlank(strDate)) {// 无缓存，从当天开始
					date = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
				} else {// 缓存的日期是已经执行过，需要+1天
					Date d = DateUtil.addDate(strDate, 1);
					date = Integer.valueOf(DateUtil.formatYYYYMMDD(d));
				}
				while (true) {
					if (tradeCalService.isOpen(date)) {
						log.info("processing date={}", date);
						run(date);
					} else {
						log.info("{}非交易日", date);
					}
					// 缓存已经处理的日期
					redisUtil.set(RedisConstant.RDS_MODEL_V1_DATE, date);
					// 新增一天
					Date d1 = DateUtil.addDate(date + "", 1);
					date = Integer.valueOf(DateUtil.formatYYYYMMDD(d1));
					if (date > today) {
						log.info("today:{},date:{} 循环结束", today, date);
						break;
					}
				}
			} else {// 手动某一天
				if (!tradeCalService.isOpen(date)) {
					log.info("{}非交易日", date);
					return;
				}
				log.info("processing date={}", date);
				List<ModelV1> deleteall = getListByCode(null, date + "", null, null, null, deleteQueryPage, null);
				log.info("删除当天{}记录条数{}", date, deleteall.size());
				esModelV1Dao.deleteAll(deleteall);
				Thread.sleep(3 * 1000);
				log.info("模型date={}开始", date);
				run(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "模型运行异常", "", "");
			WxPushUtil.pushSystem1("模型运行异常..");
		}
	}

	private void run(int treadeDate) {
		String startTime = DateUtil.getTodayYYYYMMDDHHMMSS();
		JSONArray array = tushareSpider.getStockDaliyBasic(null, treadeDate + "", null, null).getJSONArray("items");
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易daily_basic（每日指标）记录,tushare,日期={}", treadeDate);
			throw new RuntimeException("交易日但未获取到数据");
		}
		List<StockAvg> avgList = Collections.synchronizedList(new LinkedList<StockAvg>());
		List<StrategyListener> models = new LinkedList<StrategyListener>();
		models.add(new V1SortStrategyListener(treadeDate));
		models.add(new V2SortStrategyListener(treadeDate));
		models.add(new V2PRESortStrategyListener(treadeDate));
		try {
			Map<String, List<ConceptInfo>> gn = conceptService.getDailyMap(treadeDate);
			int size = array.size();
			log.info("{}获取到每日指标记录条数={}", treadeDate, size);
			CountDownLatch cunt = new CountDownLatch(array.size());
			for (int i = 0; i < array.size(); i++) {
				DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));

				ModelContext cxt = new ModelContext();
				cxt.setCode(d.getCode());
				cxt.setDate(d.getTrade_date());
				cxt.setToday(d);// 未包含全部信息-来自Tushare
				cxt.setGnDaliy(gn);

				TasksWorker2nd.add(new MyRunnable() {
					@Override
					public void running() {
						try {
							runModels(cxt, models, avgList);
						} catch (Exception e) {
							e.printStackTrace();
							ErrorLogFileUitl.writeError(e, "", "", "");
						} finally {
							cunt.countDown();
						}
					}
				});
			}
			cunt.await();// 等待执行完成
//			log.info("saveList size:{}", saveList.size());
//			if (saveList.size() > 0) {
//				esModelV1Dao.saveAll(saveList);
//			}
			log.info("avgList size:" + avgList.size());
			if (avgList.size() > 0) {
				avgService.saveStockAvg(avgList);
			}
			for (int i = 0; i < models.size(); i++) {
				StrategyListener sort = models.get(i);
				sort.fulshToFile();
				if (sort.getResultList().size() > 0) {
					esModelV1Dao.saveAll(sort.getResultList());
				}
			}
			log.info("MV1模型执行完成");
			WxPushUtil.pushSystem1(
					treadeDate + " -> MV模型执行完成！ 开始时间:" + startTime + " 结束时间：" + DateUtil.getTodayYYYYMMDDHHMMSS());
		} catch (Exception e) {
//			if (saveList.size() > 0) {
//				esModelV1Dao.saveAll(saveList);
//			}
			if (avgList.size() > 0) {
				avgService.saveStockAvg(avgList);
			}
			log.error(e.getMessage(), e);
			throw new RuntimeException("数据处理异常", e);
		}
	}

	private List<DaliyBasicInfo> getBasicList(ModelContext cxt) {
		return daliyBasicHistroyService.queryListByCodeForModel(cxt.getCode(), cxt.getDate(), queryPage).getContent();
	}

	private void runModels(ModelContext cxt, List<StrategyListener> models, List<StockAvg> avgList) {
		boolean isOk = true;
		String code = cxt.getCode();
		log.info("model V1 processing for code:{}", code);
		if (!stockBasicService.online1Year(code)) {
			cxt.setBaseDataOk("Online 上市不足1年");
			isOk = false;
		}
		List<DaliyBasicInfo> dailyList = null;
		if (isOk) {
			dailyList = getBasicList(cxt);
			if (dailyList == null || dailyList.size() < 5) {
				cxt.setBaseDataOk("每日指标记录小于5条,checkStrong get size<5");
				isOk = false;
			}
		}
		LineAvgPrice lineAvgPrice = null;
		LinePrice linePrice = null;
		LineVol lineVol = null;
		LineTickData lineTickData = null;
		if (isOk) {
			cxt.setToday(dailyList.get(0));// 包含全部信息-来自ES
			// 均价
			int lastDate = dailyList.get(dailyList.size() - 1).getTrade_date();
			lineAvgPrice = new LineAvgPrice(avgService, cxt, lastDate, avgList, dailyList);

			// 1强势:次数和差值:3/5/10/20/120/250天
			linePrice = new LinePrice(strongService, cxt, dailyList, lineAvgPrice.todayAv, lastDate);
			lineVol = new LineVol(cxt, dailyList);
			// 2交易方向:次数和差值:3/5/10/20/120/250天
			// 3程序单:次数:3/5/10/20/120/250天
			lineTickData = new LineTickData(cxt, dailyList, tickDataService);
			cxt.setPriceIndex(this.priceIndex(cxt.getToday()));
		}
		for (StrategyListener m : models) {
			m.processingModelResult(cxt, lineAvgPrice, linePrice, lineVol, lineTickData);
		}
	}

	// 收盘价介于最高价和最低价的index
	private int priceIndex(DaliyBasicInfo b) {
		PriceLife pl = priceLifeService.getPriceLife(b.getCode());
		if (b.getClose() <= pl.getLowest()) {
			return 0;
		} else if (b.getClose() >= pl.getHighest()) {
			return 100;
		} else {
			double base = pl.getHighest() - pl.getLowest();
			double diff = b.getClose() - pl.getLowest();
			int present = Double.valueOf(diff / base * 100).intValue();
			return present;
		}
	}

	public List<ModelV1> getListByCode(String code, String date, String whiteHorse, String score, String imageIndex,
			EsQueryPageReq querypage, Integer modelType) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(date)) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		}
		if (StringUtils.isNotBlank(score)) {
			bqb.must(QueryBuilders.rangeQuery("score").gte(score));
		}
		if (StringUtils.isNotBlank(imageIndex)) {
			bqb.must(QueryBuilders.matchPhraseQuery("imageIndex", 1));
		}
		if (modelType != null) {
			bqb.must(QueryBuilders.matchPhraseQuery("modelType", modelType));
		}
		if (StringUtils.isNotBlank(whiteHorse)) {
			bqb.must(QueryBuilders.matchPhraseQuery("whiteHorse", 1));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("score").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<ModelV1> page = esModelV1Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records BuyTrace");
		return null;
	}
}
