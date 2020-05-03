package com.stable.service.model.v1;

import java.util.ArrayList;
import java.util.Date;
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

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsModelV1Dao;
import com.stable.service.AvgService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.StrongService;
import com.stable.service.TickDataService;
import com.stable.service.TradeCalService;
import com.stable.service.model.StrategyListener;
import com.stable.service.model.image.ImageService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.vo.AvgVo;
import com.stable.vo.StrongVo;
import com.stable.vo.TickDataV1Vo;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.PriceLife;
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
	private final EsQueryPageReq queryPage = new EsQueryPageReq(250);
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

	public synchronized void runJob(int date) {
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
				if (date >= today) {
					log.info("today:{},date:{} 循环结束", today, date);
					return;
				}
			}
		} else {// 手动某一天
			if (!tradeCalService.isOpen(date)) {
				log.info("{}非交易日", date);
				return;
			}
			log.info("processing date={}", date);
			run(date);
		}
	}

	private void run(int treadeDate) {
		JSONArray array = tushareSpider.getStockDaliyBasic(null, treadeDate + "", null, null).getJSONArray("items");
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易daily_basic（每日指标）记录,tushare,日期={}", treadeDate);
			throw new RuntimeException("交易日但未获取到数据");
		}
		try {
			int size = array.size();
			log.info("{}获取到每日指标记录条数={}", treadeDate, size);
			List<ModelV1> saveList = new LinkedList<ModelV1>();
			List<StrategyListener> list = new ArrayList<StrategyListener>(2);
			list.add(new ImageStrategyListener());
			list.add(new V1SortStrategyListener());
			for (int i = 0; i < array.size(); i++) {
				DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));
				ModelV1 mv = new ModelV1();
				mv.setCode(d.getCode());
				mv.setDate(d.getTrade_date());
				mv.setClose(d.getClose());

				runModel(mv, list, saveList);
			}
			for (StrategyListener sl : list) {
				sl.fulshToFile();
			}
			log.info("saveList size:{}", saveList.size());
			if (saveList.size() > 0) {
				esModelV1Dao.saveAll(saveList);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException("数据处理异常", e);
		}
	}

	private void runModel(ModelV1 mv1, List<StrategyListener> list, List<ModelV1> saveList) {
		StrongVo sv = new StrongVo();
		TickDataV1Vo wv = new TickDataV1Vo();
		AvgVo av = new AvgVo();
		if (getInputData(mv1, sv, wv, av)) {
			String str = imageService.checkImg(mv1.getCode(), mv1.getDate());
			if (StringUtils.isNotBlank(str)) {
				if (str.contains(ImageService.MATCH_L2)) {
					mv1.setImageIndex(2);
				} else {
					mv1.setImageIndex(1);
				}
			}
			mv1.setScore(this.getSocre(mv1));
			if (mv1.getScore() > 0) {
				for (StrategyListener sl : list) {
					sl.condition(mv1, str, sv, wv, av);
				}
			}
			saveList.add(mv1);
		}
	}

	// **评分
	private int getSocre(ModelV1 mv) {
		int r = 0;
		// 1.均线指数排序
		r += mv.getAvgIndex();// 10,9,8,2,1
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
			if (mv.getSortPgm() > 0) {// 3.程序单
				r++;
			}
			if (mv.getSortWay() > 0) {// 4.交易方向
				r++;
			}
		}
		return r;
	}

	private boolean getInputData(ModelV1 mv1, StrongVo sv, TickDataV1Vo wv, AvgVo av) {
		if (!stockBasicService.online1Year(mv1.getCode())) {
			log.info("Online 不足1年，code={}", mv1.getCode());
			return false;
		}
		String code = mv1.getCode();
		log.info("model V1 processing for code:{}", code);
		// 1强势:次数和差值:3/5/10/20/120/250天
		DaliyBasicInfo lastDate = strongService.checkStrong(mv1, sv);
		if (lastDate == null) {
			return false;
		}

		// 2交易方向:次数和差值:3/5/10/20/120/250天
		// 3程序单:次数:3/5/10/20/120/250天
		tickDataService.tickDataCheck(mv1, wv, queryPage);
		this.priceIndex(mv1);
		avgService.checkAvg(mv1, lastDate.getTrade_date(), av);
		mv1.setId(code + mv1.getDate());
		log.info(sv);
		log.info(wv);
		log.info(av);
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
