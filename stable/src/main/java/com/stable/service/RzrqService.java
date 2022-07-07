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
import com.stable.es.dao.base.RztjDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.RzrqDaliy;
import com.stable.vo.bus.Rztj;

/**
 * 
 * 大宗
 */
@Service
//@Log4j2
public class RzrqService {
	@Autowired
	private RzrqDaliyDao rzrqDaliyDao;
	@Autowired
	private RztjDao rztjDao;

	Pageable pageable20 = PageRequest.of(EsQueryPageUtil.queryPage20.getPageNum(),
			EsQueryPageUtil.queryPage20.getPageSize());

	public boolean rzrqAvg20d(String code, double validPersentLine, double validBlance) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable20).build();

		Page<RzrqDaliy> page = rzrqDaliyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 1) {
			RzrqDaliy daliy = page.getContent().get(0);
			if (daliy.getBalance() < validBlance) {// 融资余额太少
				return false;
			}
			double t = 0.0d;
			for (int i = 0; i < page.getContent().size(); i++) {
				RzrqDaliy rd = page.getContent().get(i);
				if (i != 0) {// 除去最新的一天
					t += rd.getBalance();
				}
			}
			double avg = t / (page.getContent().size() - 1);// 除去最新的一天
			if (CurrencyUitl.cutProfit(CurrencyUitl.roundHalfUp(avg), daliy.getBalance()) > validPersentLine) {
				return true;
			}
		}
		return false;
	}

	public Rztj getLastRztj(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<Rztj> page = rztjDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		Rztj rz = new Rztj();
		rz.setCode(code);
		return rz;
	}

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

	public static void main(String[] args) {
		System.err.println(CurrencyUitl.cutProfit(CurrencyUitl.roundHalfUp(1.65), CurrencyUitl.roundHalfUp(2.50)));
	}
}
