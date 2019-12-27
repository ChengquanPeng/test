package com.stable.service;

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.EsDaliyBasicInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 日交易历史
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class DaliyBasicHistroyService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;
	@Autowired
	private TradeCalService tradeCalService;

	// 直接全量获取历史记录，不需要根据缓存来判断
	private boolean spiderDaliyDailyBasic(String today) {
		String preDate = tradeCalService.getPretradeDate(today);
		JSONArray array = tushareSpider.getStockDaliyBasic(null, today, null, null).getJSONArray("items");
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易daily_basic（每日指标）记录,tushare");
			return false;
		}
		for (int i = 0; i < array.size(); i++) {
			// System.err.println(array.getJSONArray(i).toJSONString());
			DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));
			esDaliyBasicInfoDao.save(d);

			String date = redisUtil.get(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode());
			if (StringUtils.isBlank(date)) {
				// 第一次
				String json = redisUtil.get(d.getCode());
				if (StringUtils.isNotBlank(json)) {
					StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
					date = base.getList_date();
				}
				// else未更新新股
			}
			if (StringUtils.isNotBlank(date) && !preDate.equals(date)) {
				// 补全缺失
				spiderStockDaliyBasic(d.getCode(), date, today);
			}
			redisUtil.set(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode(), d.getTrade_date());
		}
		return true;
	}

	private void spiderStockDaliyBasic(String code, String start_date, String end_date) {
		boolean hasMore = true;
		String lastDate = end_date;
		do {
			JSONObject data = tushareSpider.getStockDaliyBasic(TushareSpider.formatCode(code), null, start_date,
					lastDate);
			JSONArray array2 = data.getJSONArray("items");
			hasMore = data.getBoolean("has_more");
			if (array2 != null && array2.size() > 0) {
				for (int ij = 0; ij < array2.size(); ij++) {
					DaliyBasicInfo d2 = new DaliyBasicInfo(array2.getJSONArray(ij));
					esDaliyBasicInfoDao.save(d2);
					lastDate = d2.getTrade_date() + "";
				}
			}
			log.info("getStockDaliyBasic code:{},start_date:{},start_date:{},hasMore:{}?", code, start_date, end_date,
					hasMore);
		} while (hasMore);
	}

	/**
	 * 每日*定时任务
	 */
	public void jobSpiderAllDailyBasic() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.DAILY_BASIC, RunCycleEnum.DAY) {
			public Object mycall() {
				log.info("每日*定时任务 daily_basic [started]");
				String today = DateUtil.getTodayYYYYMMDD();
				spiderDaliyDailyBasic(today);
				log.info("每日*定时任务 daily_basic [end]");
				return null;
			}
		});
	}

	public void jobSpiderAllDailyBasic(String tradeDate) {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.DAILY_BASIC, RunCycleEnum.MANUAL) {
					public Object mycall() {
						spiderDaliyDailyBasic(tradeDate);
						return null;
					}
				});
	}

	public List<DaliyBasicInfo> queryListByCode(String code, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("trade_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<DaliyBasicInfo> page = esDaliyBasicInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}
}
