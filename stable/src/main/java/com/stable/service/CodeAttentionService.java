package com.stable.service;

import java.util.Comparator;
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
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.CodeAttentionHishDao;
import com.stable.job.MyCallable;
import com.stable.spider.ths.AttentionSpider;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.CodeAttentionHish;

import lombok.extern.log4j.Log4j2;

/**
 * 回购
 */
@Service
@Log4j2
public class CodeAttentionService {
	@Autowired
	private CodeAttentionHishDao codeAttentionHishDao;
	@Autowired
	private AttentionSpider attentionSpider;

	/**
	 * 爬虫
	 */
	public void fetchAll() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.CODE_ATTENTION, RunCycleEnum.DAY) {
					public Object mycall() {
						log.info("关注度抓包-开始");
						attentionSpider.start();
						log.info("关注度抓包-结束");
						return null;
					}

				});
	}

	public CodeAttentionHish get30DayHighAttentionDay(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage30.getPageNum(),
				EsQueryPageUtil.queryPage30.getPageSize());
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		// 全部
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		queryBuilder = queryBuilder.withPageable(pageable);
		queryBuilder = queryBuilder.withSort(sort);
		SearchQuery sq = queryBuilder.build();

		Page<CodeAttentionHish> page = codeAttentionHishDao.search(sq);
		if (page != null && !page.isEmpty()) {
			List<CodeAttentionHish> list = page.getContent();
			return list.stream().min(Comparator.comparingDouble(CodeAttentionHish::getRank)).get();
			// return list;
		}
		return null;
	}
}
