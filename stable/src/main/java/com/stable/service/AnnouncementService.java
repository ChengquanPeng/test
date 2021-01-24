package com.stable.service;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.AnnouncementHistDao;
import com.stable.vo.bus.AnnouncementHist;

import lombok.extern.log4j.Log4j2;

/**
 * 公告
 */
@Service
@Log4j2
public class AnnouncementService {
	@Autowired
	private AnnouncementHistDao announcementHistDao;

	/**
	 * 公告日
	 */
	public AnnouncementHist getLastRecordType(String code, int type, int startDate) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("type", type));
		bqb.must(QueryBuilders.rangeQuery("rptDate").gte(startDate));

		FieldSortBuilder sort = SortBuilders.fieldSort("rptDate").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<AnnouncementHist> page = announcementHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no AnnouncementHist code={}", code);
		return null;
	}
}
