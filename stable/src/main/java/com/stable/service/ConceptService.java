package com.stable.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDailyDao;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.ConceptDaily;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * 板块/概念
 */
@Service
@Log4j2
public class ConceptService {
	@Autowired
	private EsConceptDao esConceptDao;
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
			return "Rank:" + ranking + " " + name + " UP:" + CurrencyUitl.roundHalfUp(todayChange) + "%,</br>";
		}
	}

	public String getCodeConceptForCode(String code) {
		List<CodeConcept> list = getCodeConcept(code);
		if (list != null) {
			StringBuffer sb = new StringBuffer();
			for (CodeConcept cc : list) {
				sb.append(cc.getConceptName()).append(" ");
			}
			return sb.toString();
		}
		return "";
	}

	public List<CodeConcept> getCodeConcept(String code, int type) {
		EsQueryPageReq queryPage = EsQueryPageUtil.queryPage9999;
		Pageable pageable = PageRequest.of(queryPage.getPageNum(), queryPage.getPageSize());
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (type > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("type", type));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();
		return esCodeConceptDao.search(sq).getContent();
	}

	/**
	 * code 获取相关概率，板块
	 */
	public List<CodeConcept> getCodeConcept(String code) {
		return getCodeConcept(code, 0);
	}

	public String getCodeConceptStr(String code) {
		List<CodeConcept> l = getCodeConcept(code);
		String str = " ";
		if (l != null) {
			int i = 0;
			for (CodeConcept cc : l) {
				i++;
				str += cc.getConceptName() + " ";
				if (i == 3) {
					str += "<br/>";
					i = 0;
				}
			}
		}
		return str;
	}

	private List<ConceptDaily> getTopConcepts(int date) {
		EsQueryPageReq queryPage = EsQueryPageUtil.queryPage10;
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
					log.info(ci.getName());

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

	public List<CodeConcept> getCodes(String conceptId) {
		EsQueryPageReq queryPage = EsQueryPageUtil.queryPage9999;
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("conceptId", conceptId));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();
		return esCodeConceptDao.search(sq).getContent();
	}

	public Concept getConceptId(String aliasCode) {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage1;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(aliasCode)) {
//			bqb.must(QueryBuilders.matchPhraseQuery("aliasCode", aliasCode));
			bqb.must(QueryBuilders.matchPhraseQuery("aliasCode2", aliasCode));
		} else {
			return null;
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<Concept> page = esConceptDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no records aliasCode:{}", aliasCode);
		return null;
	}

	/**
	 * 根据板块/概率获取相关股票
	 */
	public List<String> listCodesByAliasCode(String aliasCode, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Concept cp = getConceptId(aliasCode);
		if (cp == null) {
			return null;
		}
		String conceptId = cp.getId();
		if (StringUtils.isNotBlank(conceptId)) {
			bqb.must(QueryBuilders.matchPhraseQuery("conceptId", conceptId));
		} else {
			return null;
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("code").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<CodeConcept> page = esCodeConceptDao.search(sq);
		if (page != null && !page.isEmpty()) {
			List<CodeConcept> list = page.getContent();
			List<String> codes = new LinkedList<String>();
			for (CodeConcept cc : list) {
				codes.add(cc.getCode());
			}
			return codes;
		}
		log.info("no records listCodeByCodeConceptId:{}", conceptId);
		return null;
	}
}
