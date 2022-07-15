package com.stable.service;

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
import com.stable.es.dao.base.RzrqDaliyDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.RzrqDaliy;

/**
 * 
 * 融资融券
 */
@Service
//@Log4j2
public class RzrqService {
	@Autowired
	private RzrqDaliyDao rzrqDaliyDao;

	Pageable pageable60 = PageRequest.of(EsQueryPageUtil.queryPage60.getPageNum(),
			EsQueryPageUtil.queryPage60.getPageSize());

	public double plan2(String code, int startDate) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable60).build();

		Page<RzrqDaliy> page = rzrqDaliyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 50) {

			// 最新一个月融资平均数(20天)
			double t1 = 0.0d;
			for (int i = 0; i < 20; i++) {
				RzrqDaliy rd = page.getContent().get(i);
				if (i != 0) {// 除去最新的一天
					t1 += rd.getBalance();
				}
			}
			double r1 = t1 / 20;

			// 上上个月的融资平均数(40-60天)
			double t2 = 0.0d;
			for (int i = page.getContent().size() - 1; i > (page.getContent().size() - 20); i--) {
				RzrqDaliy rd = page.getContent().get(i);
				if (i != 0) {// 除去最新的一天
					t2 += rd.getBalance();
				}
			}
			double r2 = t2 / 20;

			return CurrencyUitl.cutProfit(CurrencyUitl.roundHalfUp(r2), CurrencyUitl.roundHalfUp(r1));
		}
		return 0.0;
	}
}
