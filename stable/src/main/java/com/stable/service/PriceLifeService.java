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
		if (hist.getHigh() > pl.getHighest() || hist.getLow() < pl.getLowest()) {
			TradeHistInfoDaliy t1 = getHighest(code, 0, 0);
			pl.setHighest(t1.getHigh());
			pl.setHighDate(t1.getDate());

			TradeHistInfoDaliy t2 = getlowest(code, 0, 0);
			pl.setLowDate(t2.getDate());
			pl.setLowest(t2.getLow());
			saveToCache(pl);
		}
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
		PriceLife pl = localCash.get(code);
		if (pl != null) {
			return pl;
		}

		String json = redisUtil.get(RedisConstant.RDS_PRICE_LIFE + code);
		if (StringUtils.isNotBlank(json)) {
			return JSON.parseObject(json, PriceLife.class);
		}
		pl = new PriceLife();
		pl.setCode(code);
		TradeHistInfoDaliy t1 = getHighest(code, 0, 0);
		if (t1 == null) {
			return null;
		}
		pl.setHighest(t1.getHigh());
		pl.setHighDate(t1.getDate());

		TradeHistInfoDaliy t2 = getlowest(code, 0, 0);
		pl.setLowest(t2.getLow());
		pl.setLowDate(t2.getDate());

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

//	@javax.annotation.PostConstruct
//	public void testnoupYear() {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				int pre4Year = DateUtil.getPreYear(DateUtil.getTodayIntYYYYMMDD(), 4);
//				System.err.println("pre4Year:" + pre4Year);
//				int listdate = 20190412;
//				String[] codes = { "300768" };
//				System.err.println("start ==============");
//				for (String c : codes) {
//					if (listdate > pre4Year) {// ----preYearChk<listDate
//						System.err.println("online for 4 year?");
//					} else {
//						System.err.println(c + ":noupYear=" + noupYear(c, listdate) + ",noupYearstable="
//								+ (noupYearstable(c, listdate)));
//					}
//				}
//				System.err.println("end ==============");
//				System.exit(0);
//			}
//		}).start();
//	}

	public int noupYear(String code, int listdate) {
		return noupYear(code, listdate, 30, false);// 涨幅定义的波动程度，是否平稳
	}

	public int noupYearstable(String code, int listdate) {
		return noupYear(code, listdate, 20, true);// 涨幅定义的波动程度，是否平稳
	}

	private int noupYear(String code, int listdate, int rate, boolean stable) {
		// 第一种情况:一路下跌
		PriceLife pl = getPriceLife(code);
		Date now = new Date();
		int days = -365;
		int days2 = 365;
		int year = 0;
		int end = DateUtil.formatYYYYMMDDReturnInt(now);
		// System.err.println("year==>" + year);
		for (int i = 1; i <= 5; i++) {
			if (end < listdate) {
				break;
			}
			int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, i * days));
//			System.err.println("1start=" + start + ",end=" + end);
			TradeHistInfoDaliy low = getlowest(code, start, end);
			if (low != null) {// 停牌太久
				TradeHistInfoDaliy high = getHighest(code, start, end);
				pl.setLowest(low.getLow());// 设置当前年的最低水位
				int index = priceIndex(pl, high.getClosed());// 历史最高-区间最低，看看最高的是水位
				if (index <= rate) {// 涨幅定义的严格程度
					if (stable && !stable(code, start, end, days, days2)) {
						break;
					}
//					System.err.println(
//							"year==>" + year + ",index=" + index + ",high=" + high.getDate() + ",low=" + low.getDate());
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
		if (stable && year == 1) {
			return 1;
		}
		// 第二种情况:高位横盘
		double rateup = 120;
		if (stable) {
			rateup = 75;
		}
		int endt = DateUtil.formatYYYYMMDDReturnInt(now);
		for (int i = 1; i <= 5; i++) {
			int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, i * days));
			if (endt < listdate) {
				break;
			}
//			System.err.println("start=" + start + ",end=" + endt);
			if (stable && !stable(code, start, endt, days, days2)) {
				break;
			} else {
				TradeHistInfoDaliy low = getlowest(code, start, endt);
				if (low != null) {// 停牌太久
					TradeHistInfoDaliy high = getHighest(code, start, endt);
					double profit = CurrencyUitl.cutProfit(low.getLow(), high.getHigh());
					if (profit <= rateup) {// 整幅120
//						System.err.println("year==>" + year + ",high=" + high.getDate() + ",low=" + low.getDate());
					} else {
						break;
					}
				} else {
					break;
				}
			}

			year = i;
			endt = start;
		}
		if (year >= 2) {
			int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, (year == 2 ? 3 : year) * days));
			endt = DateUtil.formatYYYYMMDDReturnInt(now);
//			System.err.println("3start=" + start + ",end=" + endt);
			TradeHistInfoDaliy low = getlowest(code, start, endt);
			TradeHistInfoDaliy high = getHighest(code, start, endt);
			double profit = CurrencyUitl.cutProfit(low.getLow(), high.getHigh());
//			System.err.println("profit==>" + profit + ",high=" + high.getDate() + ",low=" + low.getDate());
			if (profit >= rateup) {
				return 0;
			}
			return year;
		}
		if (stable && year == 1) {
			return 1;
		}
		return 0;
	}

	private boolean stable(String code, int start, int end, int days, int days2) {
		// 最高点整幅不超过75%
		TradeHistInfoDaliy high1 = getHighest(code, start, end);
		TradeHistInfoDaliy low1 = getlowest(code, start, end);

		// 自然本年上涨趋势
		if (high1.getDate() > low1.getDate() && CurrencyUitl.cutProfit(low1.getLow(), high1.getHigh()) >= 75) {
			return false;
		}
		// 高点前的一年
		TradeHistInfoDaliy l2 = getlowest(code,
				DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(DateUtil.parseDate(high1.getDate()), days)),
				high1.getDate());

		if (CurrencyUitl.cutProfit(l2.getLow(), high1.getHigh()) >= 75) {
			return false;
		}
		// 低点后的一年
		TradeHistInfoDaliy h2 = getlowest(code, low1.getDate(),
				DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(DateUtil.parseDate(low1.getDate()), days2)));
		if (CurrencyUitl.cutProfit(low1.getLow(), h2.getHigh()) >= 75) {
			return false;
		}
		return true;
	}
}
