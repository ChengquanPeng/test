package com.stable.service;

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
import com.stable.utils.RedisUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.PriceLife;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
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
		if ("601001".equals(code)) {
			log.info("pl == null ? {}:{}", (pl == null), (pl != null ? pl : "null 对象"));
			log.info("hist.getHigh({}) > pl.getHighest({})", hist.getHigh(), pl.getHighest());
			log.info("hist.getLow({}) < pl.getLowest({})", hist.getLow(), pl.getLowest());
			WxPushUtil.pushSystem1("PriceLifeService.checkAndSetPrice 601001 日志断点");
		}

		int index = priceIndex(pl, code, hist.getClosed());
		redisUtil.set(RedisConstant.RDS_PRICE_LIFE_INDEX_ + code, index + "");
	}

	// 收盘价介于最高价和最低价的index
	public int priceIndex(String code, double price) {
		PriceLife pl = getPriceLife(code);
		return priceIndex(pl, code, price);
	}

	public int getLastIndex(String code) {
		return Integer.valueOf(redisUtil.get(RedisConstant.RDS_PRICE_LIFE_INDEX_ + code, "100"));
	}

	private int priceIndex(PriceLife pl, String code, double price) {
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
		TradeHistInfoDaliy ti = getHighest(code);
		if (ti == null) {
			return null;
		}
		pl.setHighest(ti.getHigh());
		pl.setHighDate(ti.getDate());
		TradeHistInfoDaliy t2 = getlowest(code);
		pl.setLowDate(t2.getDate());
		pl.setLowest(t2.getLow());

		saveToCache(pl);
		return pl;
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
	private TradeHistInfoDaliy getHighest(String code) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
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
	private TradeHistInfoDaliy getlowest(String code) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("low").order(SortOrder.ASC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliy> page = tradeHistDaliy.search(sq);
		return page.getContent().get(0);
	}
}
