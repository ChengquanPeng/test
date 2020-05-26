package com.stable.service.model.v1;

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
import com.google.common.util.concurrent.ListenableFuture;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsModelV1Dao;
import com.stable.service.ConceptService;
import com.stable.service.ConceptService.ConceptInfo;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.service.TradeCalService;
import com.stable.service.model.StrategyListener;
import com.stable.service.model.image.ImageService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.MyRunnable;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.TasksWorker2nd;
import com.stable.utils.WxPushUtil;
import com.stable.vo.ModelV1context;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.PriceLife;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ModelV1UpService {

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
	private ImageService imageService;
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
						run(date, true);
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
				run(date, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "模型运行异常", "", "");
			WxPushUtil.pushSystem1("模型运行异常..");
		}
	}

	private void run(int treadeDate, boolean isJob) {
		String startTime = DateUtil.getTodayYYYYMMDDHHMMSS();
		JSONArray array = tushareSpider.getStockDaliyBasic(null, treadeDate + "", null, null).getJSONArray("items");
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易daily_basic（每日指标）记录,tushare,日期={}", treadeDate);
			throw new RuntimeException("交易日但未获取到数据");
		}
		List<ModelV1> saveList = Collections.synchronizedList(new LinkedList<ModelV1>());
		List<StockAvg> avgList = Collections.synchronizedList(new LinkedList<StockAvg>());
		List<ModelV1context> exts = Collections.synchronizedList(new LinkedList<ModelV1context>());
		V1SortStrategyListener sort = new V1SortStrategyListener();
		try {
			Map<String, List<ConceptInfo>> gn = conceptService.getDailyMap(treadeDate);
			int size = array.size();
			log.info("{}获取到每日指标记录条数={}", treadeDate, size);
			CountDownLatch cunt = new CountDownLatch(array.size());
			for (int i = 0; i < array.size(); i++) {
				DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));

				ModelV1 mv = new ModelV1();
				mv.setCode(d.getCode());
				mv.setDate(d.getTrade_date());
				mv.setClose(d.getClose());
				ModelV1context cxt = new ModelV1context();
				cxt.setCode(d.getCode());
				exts.add(cxt);

				TasksWorker2nd.add(new MyRunnable() {
					@Override
					public void running() {
						runModel(mv, sort, saveList, avgList, gn, cxt, cunt);
					}
				});
			}
			cunt.await();// 等待执行完成
			log.info("saveList size:{}", saveList.size());
			if (saveList.size() > 0) {
				esModelV1Dao.saveAll(saveList);
			}
			if (avgList.size() > 0) {
				avgService.saveStockAvg(avgList);
			}
			sort.fulshToFile();
			sort.fulshToFile(treadeDate, exts);
			log.info("MV1模型执行完成");
			WxPushUtil.pushSystem1("MV1模型执行完成！ 开始时间:" + startTime + " 结束时间：" + DateUtil.getTodayYYYYMMDDHHMMSS());
			if (isJob) {
				imageCheck(array);
			}
		} catch (Exception e) {
			if (saveList.size() > 0) {
				esModelV1Dao.saveAll(saveList);
			}
			if (avgList.size() > 0) {
				avgService.saveStockAvg(avgList);
			}
			log.error(e.getMessage(), e);
			throw new RuntimeException("数据处理异常", e);
		}
	}

	public void imageCheck(JSONArray array) {
		ListenableFuture<?> l = TasksWorker.getInstance().getService().submit(new Runnable() {
			@Override
			public void run() {
				String startTime = DateUtil.getTodayYYYYMMDDHHMMSS();
				try {
					StrategyListener sl = new ImageStrategyListener();
					for (int i = 0; i < array.size(); i++) {
						DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));
						ModelV1 mv = new ModelV1();
						mv.setCode(d.getCode());
						mv.setDate(d.getTrade_date());
						mv.setClose(d.getClose());
						if (stockBasicService.online1Year(mv.getCode())) {
							String str = imageService.checkImg(mv.getCode(), mv.getDate());
							if (StringUtils.isNotBlank(str)) {
								if (str.contains(ImageService.MATCH_L2)) {
									mv.setImageIndex(2);
								} else {
									mv.setImageIndex(1);
								}
							}
							// 条件
							sl.condition(mv, str);
						} else {
							log.info("Online 不足1年，code={}", mv.getCode());
						}

					}
					sl.fulshToFile();// 存盘
					log.info("图片模型执行完成。");
					WxPushUtil
							.pushSystem1("图形模型执行完成！ 开始时间:" + startTime + " 结束时间：" + DateUtil.getTodayYYYYMMDDHHMMSS());
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, e.getMessage(), "", "");
					WxPushUtil.pushSystem1("图形模型执行异常！ 开始时间:" + startTime);
				}
			}
		});
		try {
			l.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runModel(ModelV1 mv1, V1SortStrategyListener sort, List<ModelV1> saveList, List<StockAvg> avgList,
			Map<String, List<ConceptInfo>> gn, ModelV1context cxt, CountDownLatch cunt) {
		StockAvg av = new StockAvg();
		if (getDataAndRunIndexs(mv1, cxt, av, avgList)) {
			mv1.setScore(this.getSocre(mv1, cxt, gn));
			if (mv1.getScore() > 0) {
				sort.condition(mv1, cxt, av);
			}
			// 大于10分
			if (mv1.getScore() >= 10) {
				saveList.add(mv1);
			}
		}
		cxt.setScore(mv1.getScore());
		cunt.countDown();
	}

	// **评分
	private int getSocre(ModelV1 mv, ModelV1context tdv, Map<String, List<ConceptInfo>> gn) {
		int r = 0;
		// 1.均线指数排序
		r += mv.getAvgIndex();// 10,9,8,2,1
		r += mv.getVolIndex();// 短线量
		// 2.强势指数排序
		r += mv.getSortStrong();// 1,3
		// 3.图形比较 1,0
		if (mv.getImageIndex() > 0) {
			r += 5;// L1
			if (mv.getImageIndex() == 2) {
				r += 10;// L2
			}
		}

		if (r > 0) {
			r += mv.getSortPgm();// 3.程序单
			r += mv.getSortWay();// 4.交易方向
			// 概念板块
			List<ConceptInfo> list = gn.get(mv.getCode());
			if (list != null) {
				int s = 0;
				for (int i = 0; i < list.size(); i++) {
					ConceptInfo x = list.get(i);
					s += x.getRanking();
					tdv.addGnStr(x.toString());
				}
				r += s;
				mv.setConceptRanking(tdv.getGnStr());
			}
		}
		return r;
	}

	private boolean getDataAndRunIndexs(ModelV1 mv1, ModelV1context cxt, StockAvg av, List<StockAvg> avgList) {
		String code = mv1.getCode();
		log.info("model V1 processing for code:{}", code);
		if (!stockBasicService.online1Year(code)) {
			cxt.setDropOutMsg("Online 上市不足1年");
			return false;
		}
		// 1强势:次数和差值:3/5/10/20/120/250天
		List<DaliyBasicInfo> dailyList = strongService.checkStrong(mv1, cxt);
		if (dailyList == null) {
			return false;
		}
		DaliyBasicInfo lastDate = dailyList.get(dailyList.size() - 1);
		// 2交易方向:次数和差值:3/5/10/20/120/250天
		// 3程序单:次数:3/5/10/20/120/250天
		tickDataService.tickDataCheck(mv1, cxt);
		this.priceIndex(mv1);
		// 均价
		avgService.checkAvg(mv1, lastDate.getTrade_date(), av, avgList, dailyList, cxt);
		// 量
		mv1.setId(code + mv1.getDate());
		// log.info(wv);
		// log.info(av);
		return true;
	}

	// 收盘价介于最高价和最低价的index
	private void priceIndex(ModelV1 mv1) {
		PriceLife pl = priceLifeService.getPriceLife(mv1.getCode());
		if (mv1.getClose() <= pl.getLowest()) {
			mv1.setPriceIndex(0);
		} else if (mv1.getClose() >= pl.getHighest()) {
			mv1.setPriceIndex(100);
		} else {
			double base = pl.getHighest() - pl.getLowest();
			double diff = mv1.getClose() - pl.getLowest();
			int present = Double.valueOf(diff / base * 100).intValue();
			mv1.setPriceIndex(present);
		}
	}

	public List<ModelV1> getListByCode(String code, String date, String score, String imageIndex,
			EsQueryPageReq querypage) {
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
