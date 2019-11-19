package com.stable.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

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
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 财务
 * @author roy
 *
 */
@Service
@Log4j2
public class FinanceService {

	@Autowired
	private ThsSpider thsSpider;
	@Autowired
	private EsFinanceBaseInfoDao esFinanceBaseInfoDao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RedisUtil redisUtil;

	public boolean spiderFinaceHistoryInfo(String code) {
		List<FinanceBaseInfo> list = thsSpider.getBaseFinance(code);
		if (list == null || list.size() <= 0) {
			log.warn("未抓取到Finane记录,code={}", code);
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

	public List<FinanceBaseInfo> getFinaceReports(String code, int pageNum, int size) {
		log.info("query code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("reportDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	private FinanceBaseInfo getLastFinaceReport(String code) {
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

	public void jobSpiderFinaceHistoryInfo() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("同步股票报告[started]");
				List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
				for (StockBaseInfo s : list) {
					String rv = redisUtil.get(RedisConstant.RDS_FINACE_HIST_INFO_ + s.getCode());
					if (StringUtils.isNotBlank(rv)) {
						continue;
					}
					if (spiderFinaceHistoryInfo(s.getCode())) {
						redisUtil.set(RedisConstant.RDS_FINACE_HIST_INFO_ + s.getCode(), "1", Duration.ofDays(1));
					}
				}
				log.info("同步股票报告[end]");
				return null;
			}
		});
	}
}
