package com.stable.service;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
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
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsShareFloatDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
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
	private RedisUtil redisUtil;
	@Autowired
	private StockBasicService stockBasicService;

	public void spiderShareFloatInfo(String start_date, String end_date) {
		TasksWorker.getInstance().getService().submit(
				new MyCallable(RunLogBizTypeEnum.SHARE_FLOAT, RunCycleEnum.MANUAL, start_date + " " + end_date) {
					public Object mycall() {
						fetchHist2(start_date);
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

	public void jobFetchHistEveryDay() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.SHARE_FLOAT, RunCycleEnum.DAY) {
			public Object mycall() {
				String rv = redisUtil.get(RedisConstant.RDS_SHARE_FLOAT_LAST_DAY);
				Date lastDate = null;
				Date todayDate = new Date();
				if (StringUtils.isBlank(rv)) {
					lastDate = DateUtil.addDate(todayDate, -1);
				} else {
					lastDate = DateUtil.parseDate(rv);
				}
				int today = Integer.valueOf(DateUtil.getYYYYMMDD(todayDate));
				do {
					lastDate = DateUtil.addDate(lastDate, 1);// 加一天
					int last = Integer.valueOf(DateUtil.getYYYYMMDD(lastDate));
					if (last > today) {
						break;
					}
					String ann_date = String.valueOf(last);
					log.info("同步限售解禁公告列表[started],ann_date={},", ann_date);
					JSONArray array = tushareSpider.getShareFloatList(null, ann_date);
					// System.err.println(array.toJSONString());
					int cnt = 0;
					if (array != null && array.size() > 0) {
						List<ShareFloat> list = new LinkedList<ShareFloat>();
						log.info("获取到限售解禁公告记录条数={}", array.size());
						for (int i = 0; i < array.size(); i++) {
							ShareFloat base = new ShareFloat(array.getJSONArray(i));
							// buyBackInfoDao.save(base);
							list.add(base);
						}
						esShareFloatDao.saveAll(list);
						cnt = list.size();
					} else {
						log.info("未获取到限售解禁公告");
					}
					redisUtil.set(RedisConstant.RDS_SHARE_FLOAT_LAST_DAY, last);
					log.info("同步限售解禁公告列表[end],ann_date={}", ann_date);
					WxPushUtil.pushSystem1(ann_date + " 获取到[" + cnt + "]条限售解禁公告！");
				} while (true);
				return null;
			}
		});
	}

	public void jobFetchHist() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.SHARE_FLOAT, RunCycleEnum.WEEK) {
			public Object mycall() {
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				int d = 0;
				if (cal.get(Calendar.DAY_OF_WEEK) == 1) {
					d = -6;
				} else {
					d = 2 - cal.get(Calendar.DAY_OF_WEEK);
				}
				d = d - 7;// 上周
				cal.add(Calendar.DAY_OF_WEEK, d);
				// 所在周开始日期
				String startDate = "";
				String endDate = "";
				int cnt = 0;
				for (int i = 1; i <= 7; i++) {
					String date = DateUtil.getYYYYMMDD(cal.getTime());
					if (i == 1) {
						startDate = date;
					} else if (i == 7) {
						endDate = date;
					}
					cnt += fetchHist2(date);
					cal.add(Calendar.DAY_OF_WEEK, 1);
				}
				WxPushUtil.pushSystem1(startDate + " " + endDate + "获取限售解禁记录条数=" + cnt);
				return null;
			}
		});
	}

	private int fetchHist2(String ann_date) {
		List<StockBaseInfo> lists = stockBasicService.getAllOnStatusList();
		int cnt = 0;
		List<ShareFloat> list = new LinkedList<ShareFloat>();
		for (StockBaseInfo sbi : lists) {
			JSONArray array = tushareSpider.getShareFloatList(sbi.getTs_code(), ann_date);
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
