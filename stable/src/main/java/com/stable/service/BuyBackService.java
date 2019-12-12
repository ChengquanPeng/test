package com.stable.service;

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

import com.alibaba.fastjson.JSONArray;
import com.stable.es.dao.EsBuyBackInfoDao;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.BuyBackInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 回购
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class BuyBackService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;

	public void spiderBuyBackHistoryInfo() {
		this.spiderBuyBackHistoryInfo(null, null);
	}

	public void spiderBuyBackHistoryInfo(String start_date, String end_date) {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				fetchHist(start_date, end_date);
				return null;
			}
		});
	}

	public void jobFetchHistEveryDay(String ann_date) {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("同步回购公告列表[started],ann_date={},", ann_date);
				JSONArray array = tushareSpider.getBuyBackList(null, null, ann_date);
				// System.err.println(array.toJSONString());
				for (int i = 0; i < array.size(); i++) {
					BuyBackInfo base = new BuyBackInfo(array.getJSONArray(i));
					// if(i==0) {
					buyBackInfoDao.save(base);
					// }
					// System.err.println(base);
				}
				log.info("同步回购公告列表[end],ann_date={}", ann_date);
				return null;
			}
		});
	}

	public void fetchHist(String start_date, String end_date) {
		log.info("同步回购公告列表[started],start_date={},end_date={},", start_date, end_date);
		JSONArray array = tushareSpider.getBuyBackList(start_date, end_date, null);
		// System.err.println(array.toJSONString());
		for (int i = 0; i < array.size(); i++) {
			BuyBackInfo base = new BuyBackInfo(array.getJSONArray(i));
			// if(i==0) {
			buyBackInfoDao.save(base);
			// }
			// System.err.println(base);
		}
		log.info("同步回购公告列表[end],start_date={},end_date={},", start_date, end_date);
	}

	public List<BuyBackInfo> getBuyBackInfo(String code, int dtype, int asc) {
		log.info("query code={},dtype={},asc={}", code, dtype, asc);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = null;
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else {
			pageable = PageRequest.of(0, 100);
		}
		// 提议/预案/股东大会通过/实施/完成/停止
		if (dtype == 0) {// 全部

		} else if (dtype == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "提议"));
		} else if (dtype == 2) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "预案"));
		} else if (dtype == 3) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "股东大会通过"));
		} else if (dtype == 4) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "实施"));
		} else if (dtype == 5) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "完成"));
		} else if (dtype == 6) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "停止"));
		}
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
