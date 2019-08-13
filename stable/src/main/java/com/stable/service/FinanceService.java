package com.stable.service;

import java.util.List;

import javax.annotation.PostConstruct;

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

import com.stable.constant.RedisConstant;
import com.stable.es.dao.EsFinanceBaseInfoDao;
import com.stable.spider.ths.ThsSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.FinanceBaseInfo;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class FinanceService {

	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private ThsSpider thsSpider;
	@Autowired
	private EsFinanceBaseInfoDao esFinanceBaseInfoDao;

	public boolean spiderFinaceHistoryInfo(String code) {
		List<FinanceBaseInfo> list = thsSpider.getBaseFinance(code);
		if (list == null || list.size() <= 0) {
			return false;
		}
		FinanceBaseInfo last = getLastFinaceReport(code);

		for (FinanceBaseInfo f : list) {
			if (last == null || f.getReportDate() > last.getReportDate()) {
				esFinanceBaseInfoDao.save(f);
				log.info("saved code={},date={}", code, f.getReportDate());
			}
		}
		return true;
	}

	public FinanceBaseInfo getLastFinaceReport(String code) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("reportDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinanceBaseInfo f = page.getContent().get(0);
			log.info("page size={},last report fince code={},date={}", page.getContent().size(), code,
					f.getReportDate());
			return f;
		}
		log.info("no last report fince code={}", code);
		return null;
	}

	//@PostConstruct
	private void test() {
		spiderFinaceHistoryInfo("600000");
	}
}
