package com.stable.service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsTradeHistInfoDaliyDao;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.PriceLife;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
//@Log4j2
public class PriceLifeService {

	private Map<String, PriceLife> localCash = new ConcurrentHashMap<String, PriceLife>();
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsTradeHistInfoDaliyDao tradeHistDaliy;

	/**
	 * 实时更新价格
	 */
	public void checkAndSetPrice(TradeHistInfoDaliy hist) {
		String code = hist.getCode();
		PriceLife pl = this.getPriceLife(code);
		if (pl == null) {
			pl = new PriceLife();
			pl.setCode(code);
			pl.setHighDate(hist.getDate());
			pl.setHighest(hist.getHigh());
			pl.setLowDate(hist.getDate());
			pl.setLowest(hist.getLow());

			saveToCache(pl);
			return;
		}
		if (hist.getHigh() > pl.getHighest()) {
			pl.setHighDate(hist.getDate());
			pl.setHighest(hist.getHigh());
			saveToCache(pl);
		}
		if (hist.getLow() < pl.getLowest()) {
			pl.setLowDate(hist.getDate());
			pl.setLowest(hist.getLow());
			saveToCache(pl);
		}
//		if ("601001".equals(code)) {
//			log.info("pl == null ? {}:{}", (pl == null), (pl != null ? pl : "null 对象"));
//			log.info("hist.getHigh({}) > pl.getHighest({})", hist.getHigh(), pl.getHighest());
//			log.info("hist.getLow({}) < pl.getLowest({})", hist.getLow(), pl.getLowest());
//			WxPushUtil.pushSystem1("PriceLifeService.checkAndSetPrice 601001 日志断点");
//		}

		int index = priceIndex(pl, hist.getClosed());
		redisUtil.set(RedisConstant.RDS_PRICE_LIFE_INDEX_ + code, index + "");
	}

	// 收盘价介于最高价和最低价的index
	public int priceIndex(String code, double price) {
		PriceLife pl = getPriceLife(code);
		return priceIndex(pl, price);
	}

	public int getLastIndex(String code) {
		return Integer.valueOf(redisUtil.get(RedisConstant.RDS_PRICE_LIFE_INDEX_ + code, "100"));
	}

	public int priceIndex(PriceLife pl, double price) {
		if (pl == null || price <= pl.getLowest()) {
			return 0;
		} else if (price >= pl.getHighest()) {
			return 100;
		} else {
			double base = pl.getHighest() - pl.getLowest();
			double diff = price - pl.getLowest();
			int present = Double.valueOf(diff / base * 100).intValue();
			return present;
		}
	}

	/**
	 * 获取历史最高价格和最低价格
	 */
	public PriceLife getPriceLife(String code) {
		if (localCash.containsKey(code)) {
			return localCash.get(code);
		}

		String json = redisUtil.get(RedisConstant.RDS_PRICE_LIFE + code);
		if (StringUtils.isNotBlank(json)) {
			return JSON.parseObject(json, PriceLife.class);
		}
		PriceLife pl = new PriceLife();
		pl.setCode(code);
		TradeHistInfoDaliy ti = getHighest(code, 0, 0);
		if (ti == null) {
			return null;
		}
		pl.setHighest(ti.getHigh());
		pl.setHighDate(ti.getDate());
		TradeHistInfoDaliy t2 = getlowest(code, 0, 0);
		pl.setLowDate(t2.getDate());
		pl.setLowest(t2.getLow());

		saveToCache(pl);
		return pl;
	}

	public PriceLife getPriceLife(String code, int enddate) {
		PriceLife pl = new PriceLife();
		pl.setCode(code);
		TradeHistInfoDaliy ti = getHighest(code, 0, enddate);// 找出历史最高价
		if (ti == null) {
			return null;
		}
		pl.setHighest(ti.getHigh());
		pl.setHighDate(ti.getDate());
		TradeHistInfoDaliy t2 = getlowest(code, ti.getDate(), enddate);// 找出历史最高价--到enddate之前的最低价
		if (t2 != null) {
			pl.setLowDate(t2.getDate());
			pl.setLowest(t2.getLow());
			return pl;
		}
		return null;
	}

	/**
	 * 复权-重新获取
	 */
	public void removePriceLifeCache(String code) {
		localCash.remove(code);
		redisUtil.del(RedisConstant.RDS_PRICE_LIFE + code);
	}

	private void saveToCache(PriceLife pl) {
		localCash.put(pl.getCode(), pl);
		redisUtil.set(RedisConstant.RDS_PRICE_LIFE + pl.getCode(), pl);
	}

	/**
	 * 获取历史最高价格
	 */
	public TradeHistInfoDaliy getHighest(String code, int start, int end) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (start > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(start).lte(end));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("high").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliy> page = tradeHistDaliy.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	/**
	 * 获取历史最低价格
	 */
	public TradeHistInfoDaliy getlowest(String code, int start, int end) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (start > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(start).lte(end));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("low").order(SortOrder.ASC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliy> page = tradeHistDaliy.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

//	@PostConstruct
	public void testnoupYear() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String code = "601500";
				int listdate = 20100101;
				System.err.println("code=" + code + ",noupYear=" + (noupYear(code, listdate, 10)));
			}
		}).start();
	}

	public int noupYear(String code, int listdate) {
		return noupYear(code, listdate, 30);// 涨幅定义的波动程度，是否平稳
	}

	public int noupYearstable(String code, int listdate) {
		return noupYear(code, listdate, 15);// 涨幅定义的波动程度，是否平稳
	}

	private int noupYear(String code, int listdate, int stable) {
		// 第一种情况:一路下跌
		PriceLife pl = getPriceLife(code);
		Date now = new Date();
		int days = -365;
		int year = 0;
		int end = DateUtil.formatYYYYMMDDReturnInt(now);

		for (int i = 1; i <= 5; i++) {
			if (end < listdate) {
				break;
			}
			int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, i * days));
			TradeHistInfoDaliy low = getlowest(code, start, end);
			if (low != null) {// 停牌太久
				TradeHistInfoDaliy high = getHighest(code, start, end);
				pl.setLowest(low.getLow());// 设置当前年的最低水位
//				pl.setLowDate(low.getDate());
				int index = priceIndex(pl, high.getClosed());
				if (index <= stable) {// 涨幅定义的严格程度
					year = i;
				} else {
					break;
				}
			} else {
				break;
			}
			end = start;
		}
		if (year >= 2) {
			return year;
		}
		// 第二种情况:横盘
		end = DateUtil.formatYYYYMMDDReturnInt(now);
		int endt = Integer.MAX_VALUE;
		for (int i = 1; i <= 5; i++) {
			int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, i * days));
			if (endt < listdate) {
				break;
			}
			TradeHistInfoDaliy low = getlowest(code, start, end);
			if (low != null) {// 停牌太久
				TradeHistInfoDaliy high = getHighest(code, start, end);
				double profit = CurrencyUitl.cutProfit(low.getLow(), high.getHigh());
				if (profit <= 120.0) {// 整幅120
					year = i;
				} else {
					break;
				}
			} else {
				break;
			}
			endt = start;
		}
		if (year >= 2) {
			return year;
		}
		return 0;
	}

//	@PostConstruct
//	private void a() {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				ThreadsUtil.sleepRandomSecBetween1And5();
//				String today = DateUtil.getTodayYYYYMMDD();
//				String[] code = { "300873" };
//				String[] code = { "002405", "002739", "600519", "002752", "300027" };
//				System.err.println("==============");
//				for (String c : code) {
//					// daliyTradeHistroyService.spiderDaliyTradeHistoryInfoFromIPOCenter(c, today,
//					// 0);
//					System.err.println(c + ":" + noupYear(c, 20150101));
//				}
//				System.err.println("==============");
//			}
//		}).start();
//	}

}
