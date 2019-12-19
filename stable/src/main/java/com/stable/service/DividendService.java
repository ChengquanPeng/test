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
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.EsDividendHistoryDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.DividendHistory;
import com.stable.vo.spi.req.DividendReq;

import lombok.extern.log4j.Log4j2;
/**
 * 描述：分红送股数据
 * @author roy
 *
 */
@Service
@Log4j2
public class DividendService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsDividendHistoryDao esDividendHistoryDao;

	private boolean spiderDividend(String ts_code, String ann_date) {
		try {
			DividendReq req = new DividendReq();
			if (StringUtils.isNotBlank(ts_code)) {
				req.setTs_code(ts_code);
			}
			if (StringUtils.isNotBlank(ann_date)) {
				req.setAnn_date(ann_date);
			}
			JSONArray array = tushareSpider.getDividend(req);

			if (array == null || array.size() <= 0) {
				log.warn("未获取到日交易记录,tushare,code={}");
				return false;
			}
			System.err.println(array);
			for (int i = 0; i < array.size(); i++) {
				JSONArray arr = array.getJSONArray(i);
				if ("实施".equals(arr.get(3))) {
					// System.err.println(arr);
					DividendHistory d = new DividendHistory(arr);
					// System.err.println(d);
					esDividendHistoryDao.save(d);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	public List<DividendHistory> getListByCode(String code) {

		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("end_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<DividendHistory> page = esDividendHistoryDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no DividendHistory for code={}", code);
		return null;

	}

	public boolean spiderDividendByCode(String code) {
		return spiderDividend(TushareSpider.formatCode(code), null);
	}

	public boolean spiderDividendByDate(String ann_date) {
		return spiderDividend(null, ann_date);
	}

	/**
	 * 每日*定时任务-日分红
	 */
	public void jobSpiderDividendByDate() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND, RunCycleEnum.DAY) {
			public Object mycall() {
				log.info("每日*定时任务-日分红公告[started]");
				spiderDividendByDate(DateUtil.getTodayYYYYMMDD());
				log.info("每日*定时任务-日分红公告[end]");
				return null;
			}
		});
	}
}
