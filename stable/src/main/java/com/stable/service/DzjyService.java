package com.stable.service;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.DzjyDao;
import com.stable.es.dao.base.DzjyYiTimeDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.DzjyYiTime;

/**
 * 
 * 大宗
 */
@Service
//@Log4j2
public class DzjyService {
	@Autowired
	private DzjyDao dzjyDao;
	@Autowired
	private DzjyYiTimeDao dzjyYiTimeDao;

	/**
	 * 一段时间内的交易额
	 * 
	 * @param startDate 开始到现在
	 */
	public DzjyYiTime halfOver1Yi(String code, int startDate) {
		DzjyYiTime t = new DzjyYiTime();
		t.setCode(code);
		t.setTotalAmt(0.0);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			double num = 0.0;
			for (Dzjy d : page.getContent()) {
				num += d.getTvol();
				t.setTotalAmt(t.getTotalAmt() + d.getTval());
			}
			t.setAvgPrcie(CurrencyUitl.roundHalfUp(t.getTotalAmt() / num));
			t.setDate(page.getContent().get(0).getDate());
		}
		return t;
	}

	public void halfOver60d(String code, int startDate, DzjyYiTime t) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			for (Dzjy d : page.getContent()) {
				t.setTotalAmt60d(t.getTotalAmt60d() + d.getTval());
			}
		}
	}

	public DzjyYiTime dzjyF(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(date));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<DzjyYiTime> page = dzjyYiTimeDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}
}
