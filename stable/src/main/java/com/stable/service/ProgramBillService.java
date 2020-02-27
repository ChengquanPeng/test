package com.stable.service;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.EsTickDataBuySellInfoDao;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ProgramBillService {
	@Autowired
	private EsTickDataBuySellInfoDao esTickDataBuySellInfoDao;

	public Page<TickDataBuySellInfo> aa(String code, String date, String programRate, EsQueryPageReq queryPage) {

		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},date={},programRate={},pageNum={},size={}", code, date, programRate, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("date").gte("start").lte("end"));
		bqb.must(QueryBuilders.rangeQuery("programRate").gt(Integer.valueOf(0)));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<TickDataBuySellInfo> page = esTickDataBuySellInfoDao.search(sq);
		return page;

	}
}
