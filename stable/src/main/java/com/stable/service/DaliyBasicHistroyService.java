package com.stable.service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.MyRunnable;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.TasksWorker2nd;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.DaliyBasicInfoResp;
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
	@Autowired
	private StockBasicService stockBasicService;

	@Value("${tick.data.start.date}")
	public String startDate;

	// 直接全量获取历史记录，不需要根据缓存来判断
	private synchronized boolean spiderDaliyDailyBasic(String today) {
		String preDate = tradeCalService.getPretradeDate(today);
		JSONArray array = tushareSpider.getStockDaliyBasic(null, today, null, null).getJSONArray("items");
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易daily_basic（每日指标）记录,tushare,日期={}", today);
			return false;
		}

		try {
			int size = array.size();
			log.info("{}获取到每日指标记录条数={}", today, size);
			CountDownLatch cnt = new CountDownLatch(size);
			List<DaliyBasicInfo> list = new LinkedList<DaliyBasicInfo>();
			for (int i = 0; i < array.size(); i++) {
				// System.err.println(array.getJSONArray(i).toJSONString());
				DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));
				list.add(d);

				int index = i;
				TasksWorker2nd.add(new MyRunnable() {
					public void running() {
						try {
							log.info("<每日指标记录>正在处理code={}", d.getCode());
							String date = redisUtil.get(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode());
							if (String.valueOf(d.getTrade_date()).equals(date)) {
								log.info("<每日指标记录>不需要处理,code={},lastDate={},index={}", d.getCode(), date, index);
								return;
							}
							if (StringUtils.isBlank(date)) {
								// 第一次
								String json = redisUtil.get(d.getCode());
								if (StringUtils.isNotBlank(json)) {
									StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
									date = base.getList_date();
								}
								// else未更新新股
							}

							if (StringUtils.isNotBlank(date) && !preDate.equals(date) && !date.equals(today)) {
								log.info("<每日指标记录>需要重新获取或者补全 code={},date={},preDate={},index={}", d.getCode(), date,
										preDate, index);
								final String datep = date;

								// 补全缺失
								spiderStockDaliyBasic(d.getCode(), datep, today);
								redisUtil.set(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode(), d.getTrade_date());

							} else {
								redisUtil.set(RedisConstant.RDS_TRADE_DAILY_BASIC_ + d.getCode(), d.getTrade_date());
								log.info("<每日指标记录>code={}已完成,最后更新交易日={},index={}", d.getCode(), d.getTrade_date(),
										index);
							}

						} finally {
							cnt.countDown();
						}
					}
				});
			}
			esDaliyBasicInfoDao.saveAll(list);
			cnt.await();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	/**
	 * 日线行情：昨收，最高，开盘，最低，交易量，交易额
	 */
	public void getDailyData(DaliyBasicInfo d) {
		try {
			JSONArray array = tushareSpider.getStockDaliyTrade(d.getTs_code(), d.getTrade_date() + "", null, null);
			d.daily(array.getJSONArray(0));
		} catch (Exception e) {
			log.warn("get exception:" + e.getMessage());
		}
	}

	public void spiderStockDaliyBasic(String code, String start_date, String end_date) {
		boolean hasMore = true;
		String lastDate = end_date;
		do {
			JSONObject data = tushareSpider.getStockDaliyBasic(TushareSpider.formatCode(code), null, start_date,
					lastDate);
			JSONArray array2 = data.getJSONArray("items");
			hasMore = data.getBoolean("has_more");
			if (array2 != null && array2.size() > 0) {
				List<DaliyBasicInfo> list = new LinkedList<DaliyBasicInfo>();
				for (int ij = 0; ij < array2.size(); ij++) {
					DaliyBasicInfo d2 = new DaliyBasicInfo(array2.getJSONArray(ij));
					list.add(d2);
					// tickDataService.sumTickData(d2);
					lastDate = d2.getTrade_date() + "";

					if (list.size() > 2000) {
						esDaliyBasicInfoDao.saveAll(list);
						list = new LinkedList<DaliyBasicInfo>();
					}
				}
				if (list.size() > 0) {
					esDaliyBasicInfoDao.saveAll(list);
				}
			}
			log.info("getStockDaliyBasic code:{},start_date:{},end_date:{},hasMore:{}?", code, start_date, end_date,
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

	public List<DaliyBasicInfoResp> queryListByCodeByWebPage(String code, EsQueryPageReq queryPage) {
		List<DaliyBasicInfoResp> res = new LinkedList<DaliyBasicInfoResp>();
		Page<DaliyBasicInfo> page = this.queryListByCode(code, null, null, queryPage);
		if (page != null && !page.isEmpty()) {
			for (DaliyBasicInfo dh : page.getContent()) {
				DaliyBasicInfoResp resp = new DaliyBasicInfoResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public Page<DaliyBasicInfo> queryListByCode(String code, String date, String fetchTickData,
			EsQueryPageReq queryPage, SortOrder order) {

		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},trade_date={},fetchTickData={},pageNum={},size={}", code, date, fetchTickData,
				pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(date)) {
			bqb.must(QueryBuilders.matchPhraseQuery("trade_date", Integer.valueOf(date)));
		}
		if (StringUtils.isNotBlank(fetchTickData)) {
			bqb.must(QueryBuilders.matchPhraseQuery("fetchTickData", Integer.valueOf(fetchTickData)));
			bqb.must(QueryBuilders.rangeQuery("trade_date").from(startDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("trade_date").unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esDaliyBasicInfoDao.search(sq);
	}

	public List<DaliyBasicInfo> queryListByCode(String code, int startDate, int endDate, SortOrder order) {
		Pageable pageable = PageRequest.of(0, 10000);
		log.info("queryPage code={},startDate={},endDate={}", code, startDate, endDate);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("trade_date").gte(startDate));
		bqb.must(QueryBuilders.rangeQuery("trade_date").lte(endDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("trade_date").unmappedType("integer").order(order);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esDaliyBasicInfoDao.search(sq).getContent();
	}

	public Page<DaliyBasicInfo> queryListByCode(String code, String date, String fetchTickData,
			EsQueryPageReq queryPage) {
		return this.queryListByCode(code, date, fetchTickData, queryPage, SortOrder.DESC);
	}
}
