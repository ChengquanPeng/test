package com.stable.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDailyDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.ConceptDaily;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * 概念
 */
@Service
@Log4j2
public class ConceptService {
	@Autowired
	private EsCodeConceptDao esCodeConceptDao;
	@Autowired
	private EsConceptDailyDao esConceptDailyDao;

	@Getter
	@Setter
	public class ConceptInfo {

		private String name;
		private int ranking;
		private double todayChange;

		@Override
		public String toString() {
			return name + " 涨幅:" + CurrencyUitl.roundHalfUp(todayChange) + "%,排名:" + ranking;
		}

	}

	private List<ConceptDaily> getTopConcepts(int date) {
		EsQueryPageReq queryPage = new EsQueryPageReq(10);
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		bqb.must(QueryBuilders.rangeQuery("todayChange").gt(0.0));
		FieldSortBuilder sort = SortBuilders.fieldSort("ranking").order(SortOrder.ASC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esConceptDailyDao.search(sq).getContent();
	}

	public Map<String, List<ConceptInfo>> getDailyMap(int date) {
		Map<String, List<ConceptInfo>> m = new HashMap<String, List<ConceptInfo>>();
		List<ConceptDaily> list = this.getTopConcepts(date);
		log.info("获取概念数量：" + list.size());
		if (list != null && list.size() > 0) {
			list.forEach(daily -> {
				String conceptId = daily.getConceptId();
				List<CodeConcept> codes = this.getCodes(conceptId);
				if (codes.size() > 0) {
					ConceptInfo ci = new ConceptInfo();
					ci.setName(codes.get(0).getConceptName());
					ci.setRanking(daily.getRanking());
					ci.setTodayChange(daily.getTodayChange());
					list.forEach(x -> {
						log.info(ci.getName());
					});
					codes.forEach(c -> {
						List<ConceptInfo> l = m.get(c.getCode());
						if (l == null) {
							l = new LinkedList<ConceptInfo>();
						}
						l.add(ci);
						m.put(c.getCode(), l);
					});
				}
			});
		}
		log.info("代码数量：" + m.keySet().size());
		return m;
	}

	private List<CodeConcept> getCodes(String conceptId) {
		EsQueryPageReq queryPage = new EsQueryPageReq(1000);
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("conceptId", conceptId));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();
		return esCodeConceptDao.search(sq).getContent();
	}

}
