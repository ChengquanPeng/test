package com.stable.service;

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

import com.stable.es.dao.base.EsBuyBackInfoDao;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 回购
 */
@Service
@Log4j2
public class BuyBackService {
	private static final String DONE = "完成";
	private static final String STOP = "停止";
	public static final String GDDH = "股东大会通过";
	public static final String SS = "实施";
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;
	@Autowired
	private StockBasicService stockBasicService;

	public BuyBackInfo getLastRecordByLteDate(String code, int start, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("ann_date").gte(start).lte(date));
		bqb.must(QueryBuilders.rangeQuery("amount").gte(10000000));// 超过1千万
		bqb.must(QueryBuilders.termsQuery("proc", GDDH, SS, DONE));// 股东大会通过

		FieldSortBuilder sort = SortBuilders.fieldSort("ann_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<BuyBackInfo> page = buyBackInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no BuyBackInfo date={}", date);
		return null;
	}

	public List<BuyBackInfo> getBuyBackInfo(String code, int dtype, int asc, EsQueryPageReq querypage) {
		log.info("query code={},dtype={},asc={}", code, dtype, asc);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		// 提议/预案/股东大会通过/实施/完成/停止
		if (dtype == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "提议"));
		} else if (dtype == 2) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "预案"));
		} else if (dtype == 3) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", GDDH));
		} else if (dtype == 4) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", SS));
		} else if (dtype == 5) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", DONE));
		} else if (dtype == 6) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", STOP));
		}
		// 全部
		SortOrder s = SortOrder.DESC;
		if (asc == 1) {
			s = SortOrder.ASC;
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("ann_date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		if (pageable != null) {
			queryBuilder = queryBuilder.withPageable(pageable);
		}
		queryBuilder = queryBuilder.withSort(sort);
		SearchQuery sq = queryBuilder.build();

		Page<BuyBackInfo> page = buyBackInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}
}
