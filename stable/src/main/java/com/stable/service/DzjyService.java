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
import com.stable.es.dao.base.RzrqDaliyDao;
import com.stable.es.dao.base.RztjDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.bus.RzrqDaliy;
import com.stable.vo.bus.Rztj;
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
	private RzrqDaliyDao rzrqDaliyDao;
	@Autowired
	private DzjyYiTimeDao dzjyYiTimeDao;
	@Autowired
	private RztjDao rztjDao;

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
			if (s.getFloatShare() > 0) {
				t.setP365d(CurrencyUitl.roundHalfUp(num / s.getFloatShare()));// 都是万股
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
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			double num = 0.0;
			for (Dzjy d : page.getContent()) {
				num += d.getTvol();
				t.setTotalAmt60d(CurrencyUitl.roundHalfUp(t.getTotalAmt60d() + d.getTval()));
			}
			if (s.getFloatShare() > 0) {
				t.setP60d(CurrencyUitl.roundHalfUp(num / s.getFloatShare()));// 都是万股
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

	int pageNum1 = EsQueryPageUtil.queryPage20.getPageNum();
	int size1 = EsQueryPageUtil.queryPage20.getPageSize();
	Pageable pageable1 = PageRequest.of(pageNum1, size1);

	public boolean rzrqAvg20d(String code, double validPersentLine, double validBlance, Rztj rztj) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable1).build();

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
			rztj.setTotalAmt(daliy.getBalance());
			rztj.setAvgAmt(CurrencyUitl.roundHalfUp(avg));
			if (CurrencyUitl.cutProfit(rztj.getAvgAmt(), daliy.getBalance()) > validPersentLine) {
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
}
