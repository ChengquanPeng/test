package com.stable.service.trace;

import java.util.List;

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

import com.stable.es.dao.base.EsBuyTraceDao;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class BuyTraceService {

	@Autowired
	private EsBuyTraceDao esBuyTraceDao;

	public void addToTrace(BuyTrace bt) {
		esBuyTraceDao.save(bt);
	}

	public void addToTrace(List<BuyTrace> bts) {
		esBuyTraceDao.saveAll(bts);
	}

	public List<BuyTrace> getListByCode(String code, int buyDate, int status, int buyModelType,
			EsQueryPageReq querypage) {
		FieldSortBuilder sort = SortBuilders.fieldSort("buyDate").unmappedType("integer").order(SortOrder.DESC);
		return getListByCode(code, buyDate, status, buyModelType, querypage, sort);
	}

	public List<BuyTrace> getListByCode(String code, int buyDate, int status, int buyModelType,
			EsQueryPageReq querypage, FieldSortBuilder sort) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}

		if (buyDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("buyDate").gte(buyDate));
		}
		if (status > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("status", status));
		}
		if (buyModelType > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("buyModelType", buyModelType));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<BuyTrace> page = esBuyTraceDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records BuyTrace");
		return null;
	}
}
