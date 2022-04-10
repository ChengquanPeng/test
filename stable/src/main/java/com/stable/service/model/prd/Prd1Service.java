package com.stable.service.model.prd;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.OnlineTestingDao;
import com.stable.es.dao.base.Prd1Dao;
import com.stable.vo.Prd1Monitor;
import com.stable.vo.bus.OnlineTesting;
import com.stable.vo.bus.Prd1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class Prd1Service {
	@Autowired
	private Prd1Dao prd1Dao;
	@Autowired
	private OnlineTestingDao onlineTestingDao;

	public List<Prd1Monitor> getMonitorList() {
		List<Prd1Monitor> la = new LinkedList<Prd1Monitor>();
		Map<String, Prd1Monitor> map = new HashMap<String, Prd1Monitor>();
		List<Prd1> l1 = getWaitingBuyList();
		List<OnlineTesting> l2 = getBuyedList();
		for (Prd1 p1 : l1) {
			Prd1Monitor pm = new Prd1Monitor();
			pm.setCode(p1.getCode());
			pm.setBuy(true);
			map.put(pm.getCode(), pm);
		}
		for (OnlineTesting p1 : l2) {
			Prd1Monitor pm = map.get(p1.getCode());
			if (pm == null) {
				pm = new Prd1Monitor();
				pm.setCode(p1.getCode());
				map.put(pm.getCode(), pm);
			}
			pm.setOnlineTesting(p1);
		}
		for (Prd1Monitor pm : map.values()) {
			la.add(pm);
		}
		return la;
	}

	// 待买入监听列表
	private List<Prd1> getWaitingBuyList() {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("prd", 1));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<Prd1> page = prd1Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no Prd1 For Listening");
		return new LinkedList<Prd1>();
	}

	// 已买入监听列表
	private List<OnlineTesting> getBuyedList() {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("stat", 1));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<OnlineTesting> page = onlineTestingDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no OnlineTesting For Listening");
		return new LinkedList<OnlineTesting>();
	}
}
