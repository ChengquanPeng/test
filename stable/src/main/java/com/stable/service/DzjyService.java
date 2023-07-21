package com.stable.service;

import java.util.List;

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
import com.stable.utils.DateUtil;
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
	Pageable pageable1 = PageRequest.of(EsQueryPageUtil.queryPage1.getPageNum(),
			EsQueryPageUtil.queryPage1.getPageSize());

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

	// chkDate之前的数据
	public boolean chkDzjyV2(String code, int chkDate) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(chkDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty()) {
			List<Dzjy> list = page.getContent();
			if (list.size() > 0) {
				if (list.size() > 1) {
					int date1 = 0;
					int date2 = 0;

					for (int i = 1; i < list.size(); i++) {
						date1 = list.get(i - 1).getDate();
						date2 = list.get(i).getDate();

						if (DateUtil.differentDays(date1, date2) >= 360) {// 间隔一年
							return true;
						}
					}
				}

				// 最后一条数据在检查一遍
				int minDate = list.get(list.size() - 1).getDate();
				int lastChkDate = chkDzjyV2Ext(code, minDate);
				return DateUtil.differentDays(lastChkDate, minDate) >= 360;
			}
		}
		return false;
	}

	// minDate之前的数据
	public int chkDzjyV2Ext(String code, int minDate) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lt(minDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable1).withSort(sort).build();

		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			return page.getContent().get(0).getDate();
		}
		return 20120101;
	}
}
