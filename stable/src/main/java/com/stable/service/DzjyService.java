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
import com.stable.es.dao.base.DzjyDao;
import com.stable.es.dao.base.DzjyYiTimeDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.bus.StockBaseInfo;

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
	Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
			EsQueryPageUtil.queryPage9999.getPageSize());

	/**
	 * 一段时间内的交易额
	 * 
	 * @param startDate 开始到现在
	 */
	public DzjyYiTime halfOver1Yi(StockBaseInfo s, int startDate, int startDate2) {
		String code = s.getCode();
		DzjyYiTime t = new DzjyYiTime();
		t.setCode(code);
		t.setTotalAmt(0.0);
		t.setAvgPrcie(0.0);
		t.setDate(0);
		t.setTotalAmt60d(0.0);
		t.setP365d(0.0);
		t.setP60d(0.0);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			double num = 0.0;// 量
			for (Dzjy d : page.getContent()) {
				num += d.getTvol();
				t.setTotalAmt(t.getTotalAmt() + d.getTval());
			}
			t.setAvgPrcie(CurrencyUitl.roundHalfUp(t.getTotalAmt() / num));
			t.setDate(page.getContent().get(0).getDate());// 最新大宗日期
			if (s.getFloatShare() > 0) {
				double unP5liutonggf = s.getFloatShare();
				if (s.getCircZb() > 0) {// 除去5%以上的占比
					unP5liutonggf = ((100 - s.getCircZb()) * unP5liutonggf) / 100;
				}
				t.setP365d(CurrencyUitl.roundHalfUp((num / 10000 / unP5liutonggf * 100)));// num=万股/unP5liutonggf=亿股,百分比
			}
			this.halfOver60d(code, s, startDate2, t);// 2个月
		}
		return t;
	}

	public void halfOver60d(String code, StockBaseInfo s, int startDate, DzjyYiTime t) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			double num = 0.0;
			for (Dzjy d : page.getContent()) {
				num += d.getTvol();
				t.setTotalAmt60d(CurrencyUitl.roundHalfUp(t.getTotalAmt60d() + d.getTval()));
			}
			if (s.getFloatShare() > 0) {
				double unP5liutonggf = s.getFloatShare();
				if (s.getCircZb() > 0) {// 除去5%以上的占比
					unP5liutonggf = ((100 - s.getCircZb()) * unP5liutonggf) / 100;
				}
				t.setP60d(CurrencyUitl.roundHalfUp(num / 10000 / unP5liutonggf * 100));// num=万股/unP5liutonggf=亿股,百分比
			}
		}
	}

	public DzjyYiTime dzjyF(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<DzjyYiTime> page = dzjyYiTimeDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new DzjyYiTime();
	}

}
