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

import com.stable.es.dao.base.EsBuyBackInfoDao;
import com.stable.vo.bus.BuyBackInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 回购、增持
 */
@Service
@Log4j2
public class BuyBackService {
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;

	public String getLastRecordBuyBack(String code, int date) {
		BuyBackInfo bb = getLastRecord(code, 2, date);// 1.增持，2.回购
		if (bb != null) {
			return bb.getDate() + " " + bb.getDesc().split("；")[0];
		}
		return "";
	}

	public String getLastRecordZc(String code, int date) {
		BuyBackInfo bb = getLastRecord(code, 1, date);// 1.增持，2.回购
		if (bb != null) {
			try {
				return bb.getDate() + " " + bb.getDesc().substring(0, 4);
			} catch (Exception e) {
				return bb.getDate() + " " + bb.getDesc();
			}
		}
		return "";
	}

	private BuyBackInfo getLastRecord(String code, int type, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("type", type));
		bqb.must(QueryBuilders.rangeQuery("date").gte(date));

		FieldSortBuilder sort = SortBuilders.fieldSort("date").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<BuyBackInfo> page = buyBackInfoDao.search(sq);
		if (page.getContent() != null && !page.getContent().isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no BuyBackInfo ={}", code);
		return null;
	}
}
