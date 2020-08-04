package com.stable.service;

import java.util.LinkedList;
import java.util.List;

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
import com.stable.es.dao.base.EsShareFloatDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.TasksWorker;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.ShareFloat;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 限售股解禁
 */
@Service
@Log4j2
public class ShareFloatService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsShareFloatDao esShareFloatDao;
	@Autowired
	private StockBasicService stockBasicService;

	public void spiderShareFloatInfo(String start_date, String end_date) {
		TasksWorker.getInstance().getService().submit(
				new MyCallable(RunLogBizTypeEnum.SHARE_FLOAT, RunCycleEnum.MANUAL, start_date + " " + end_date) {
					public Object mycall() {
						fetchHist3();
						return null;
					}
				});
	}

	/**
	 * 按月抓取限售解禁信息，从当前月到19900101
	 */
	public void fetchAll() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.SHARE_FLOAT, RunCycleEnum.MANUAL, "fetchAll from 20190101") {
					public Object mycall() {
						List<StockBaseInfo> lists = stockBasicService.getAllOnStatusList();
						int cnt = 0;
						List<ShareFloat> list = new LinkedList<ShareFloat>();
						for (StockBaseInfo sbi : lists) {
							JSONArray array = tushareSpider.getShareFloatList(sbi.getTs_code(), null);
							if (array != null && array.size() > 0) {
								log.info("{},获取到限售解禁公告记录条数={}", sbi.getTs_code(), array.size());
								for (int i = 0; i < array.size(); i++) {
									ShareFloat base = new ShareFloat(array.getJSONArray(i));
									list.add(base);
								}
							} else {
								log.info("{}未获取到限售解禁公告", sbi.getTs_code());
							}
							if (list.size() > 1000) {
								esShareFloatDao.saveAll(list);
								cnt += list.size();
								list = new LinkedList<ShareFloat>();
							}
						}
						if (list.size() > 0) {
							esShareFloatDao.saveAll(list);
							cnt += list.size();
						}
						WxPushUtil.pushSystem1("fetchAll-By AllOnStatusList 获取限售解禁记录条数=" + cnt);
						return null;
					}

				});
	}

	public void jobFetchHist() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.SHARE_FLOAT, RunCycleEnum.WEEK) {
			public Object mycall() {
				WxPushUtil.pushSystem1("每周获取最新的限售解禁记录条数=" + fetchHist3());
				return null;
			}
		});
	}

	private int fetchHist3() {
		List<StockBaseInfo> lists = stockBasicService.getAllOnStatusList();
		int cnt = 0;
		List<ShareFloat> list = new LinkedList<ShareFloat>();
		for (StockBaseInfo sbi : lists) {
			JSONArray array = tushareSpider.getShareFloatList(sbi.getTs_code(), null);
			if (array != null && array.size() > 0) {
				log.info("{},获取到限售解禁公告记录条数={}", sbi.getTs_code(), array.size());
				for (int i = 0; i < array.size(); i++) {
					ShareFloat base = new ShareFloat(array.getJSONArray(i));
					list.add(base);
				}
			} else {
				log.info("{}未获取到限售解禁公告", sbi.getTs_code());
			}
			if (list.size() > 1000) {
				esShareFloatDao.saveAll(list);
				cnt += list.size();
				list = new LinkedList<ShareFloat>();
			}
		}
		if (list.size() > 0) {
			esShareFloatDao.saveAll(list);
			cnt += list.size();
		}
		return cnt;
	}

	public ShareFloat getLastRecordByLteDate(String code, int start, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("floatDate").gte(start).lte(date));
		FieldSortBuilder sort = SortBuilders.fieldSort("floatDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<ShareFloat> page = esShareFloatDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no ShareFloat code={}, date={}", code, date);
		return null;
	}
}
