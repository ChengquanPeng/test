package com.stable.service;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.DzjyYiTimeDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.http.resp.DzjyResp;
import com.stable.vo.spi.req.EsQueryPageReq;

/**
 * 筹码-大宗交易
 */
@Service
public class ChipsDzjyService {
	@Autowired
	private DzjyYiTimeDao dzjyYiTimeDao;
	@Autowired
	private StockBasicService stockBasicService;

	public List<DzjyResp> getDzjyTimeListForWeb(String code, int date, int totalAmt, int totalAmt60d,
			EsQueryPageReq querypage) {

		List<DzjyYiTime> list = this.getDzjyTimeList(code, date, totalAmt, totalAmt60d, querypage);
		if (list != null) {
			List<DzjyResp> l = new LinkedList<DzjyResp>();
			for (DzjyYiTime t : list) {
				DzjyResp r = new DzjyResp();
				BeanUtils.copyProperties(t, r);
				r.setCodeName(stockBasicService.getCodeName(t.getCode()));
				r.setTotalAmts(CurrencyUitl.covertToString(t.getTotalAmt() * WAN));
				r.setTotalAmt60ds(CurrencyUitl.covertToString(t.getTotalAmt60d() * WAN));
				l.add(r);
			}
			return l;
		}
		return null;
	}

	public static long WAN = CurrencyUitl.WAN_N.longValue();

	public List<DzjyYiTime> getDzjyTimeList(String code, int date, int totalAmt, int totalAmt60d,
			EsQueryPageReq querypage) {
		int pageNum = querypage.getPageNum();
		int size = querypage.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (date > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").from(date));
		}
		if (totalAmt > 0) {
			bqb.must(QueryBuilders.rangeQuery("totalAmt").gte(totalAmt * WAN));
		}
		if (totalAmt60d > 0) {
			bqb.must(QueryBuilders.rangeQuery("totalAmt60d").gte(totalAmt60d * WAN));
		}
		FieldSortBuilder sort = null;
		if (date > 0) {
			sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		} else if (totalAmt60d > 0 && totalAmt <= 0) {
			sort = SortBuilders.fieldSort("totalAmt60d").order(SortOrder.DESC);
		} else {
			sort = SortBuilders.fieldSort("totalAmt").order(SortOrder.DESC);
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<DzjyYiTime> page = dzjyYiTimeDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

}
