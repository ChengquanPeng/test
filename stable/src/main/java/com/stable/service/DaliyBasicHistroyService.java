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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.http.resp.DaliyBasicInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 日交易历史(每日指标)
 *
 */
@Service
@Log4j2
public class DaliyBasicHistroyService {
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;
	@Autowired
	private StockBasicService stockBasicService;

	@Value("${tick.data.start.date}")
	public String startDate;

	public List<DaliyBasicInfoResp> queryListByCodeByWebPage(String code, EsQueryPageReq queryPage) {
		List<DaliyBasicInfoResp> res = new LinkedList<DaliyBasicInfoResp>();
		Page<DaliyBasicInfo2> page = this.queryListByCode(code, null, queryPage);
		if (page != null && !page.isEmpty()) {
			for (DaliyBasicInfo2 dh : page.getContent()) {
				DaliyBasicInfoResp resp = new DaliyBasicInfoResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public Page<DaliyBasicInfo2> queryListByCode(String code, String date, EsQueryPageReq queryPage, SortOrder order) {

		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},date={},pageNum={},size={}", code, date, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(date)) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", Integer.valueOf(date)));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esDaliyBasicInfoDao.search(sq);
	}

	public Page<DaliyBasicInfo2> queryListByCode(String code, String date, EsQueryPageReq queryPage) {
		return this.queryListByCode(code, date, queryPage, SortOrder.DESC);
	}

	public DaliyBasicInfo2 queryLastest(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (date > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		try {
			return esDaliyBasicInfoDao.search(sq).getContent().get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public DaliyBasicInfo2 queryLastest(String code) {
		return queryLastest(code, 0);
	}

	public List<DaliyBasicInfo2> queryListByCodeForModel(String code, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		// log.info("queryPage code={},date={},pageNum={},size={}", code, date,
		// pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esDaliyBasicInfoDao.search(sq).getContent();
	}

	public Page<DaliyBasicInfo2> queryListByCodeForModel(String code, int date, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		// log.info("queryPage code={},date={},pageNum={},size={}", code, date,
		// pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esDaliyBasicInfoDao.search(sq);
	}

	public List<DaliyBasicInfo2> queryListByCode(String code, int startDate, int endDate, EsQueryPageReq queryPage,
			SortOrder s) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},startDate={},endDate={},", code, startDate, endDate);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}
		if (endDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").lte(endDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return esDaliyBasicInfoDao.search(sq).getContent();
	}
}
