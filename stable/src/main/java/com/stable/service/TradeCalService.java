package com.stable.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsTradeCalDao;
import com.stable.job.MyCallable;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.TradeCal;

import lombok.extern.log4j.Log4j2;

/**
 * 交易日历
 */
@Service
@Log4j2
@DependsOn("SpringUtil")
public class TradeCalService {

	@Autowired
	private EsTradeCalDao calDao;

	private static String url = "http://www.szse.cn/api/report/exchange/onepersistenthour/monthList?month=%s&random=%s";

//	@PostConstruct
	public void josSynTradeCal() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.TRADE_CAL, RunCycleEnum.MONTH) {
			public Object mycall() {
				log.info("开始同步日历[Month-started]");
				getFormWeb();
				log.info("开始同步日历[Month-end]");
				return null;
			}
		});
	}

	private void getFormWeb() {
		int times = 3;// 默认当前月+后两月
		int date = DateUtil.getTodayIntYYYYMMDD();
		int year = DateUtil.getYear(date);
		int curmonth = DateUtil.getMonth(date);
		if (curmonth == 1) {
			times = 12;// 整年
		}

		List<TradeCal> list = new LinkedList<TradeCal>();

		for (int k = 0; k < times; k++) {
			String ms = year + "-" + getFormatMonth(curmonth);
			log.info("日历抓取月份:" + ms);

			JSONObject result = HttpUtil.doGet(String.format(url, ms, Math.random()));
			JSONArray data = (JSONArray) result.get("data");

			for (int i = 0; i < data.size(); i++) {
				JSONObject row = data.getJSONObject(i);
				TradeCal tc = new TradeCal();
				tc.setCal_date(DateUtil.convertDate2(row.getString("jyrq")));// 日期
				tc.setIs_open(row.getIntValue("jybz"));// 是否开市
				tc.setUpdate_time(date);
				// System.err.println(tc);
				list.add(tc);
			}

			curmonth++;// 下一月
			if (curmonth >= 13) {// 跨年
				curmonth = 1;
				year++;
			}
		}
		log.info(times + ",日历抓取条数:" + list.size());
		if (list.size() > 0) {
			calDao.saveAll(list);
		}
	}

	private static String getFormatMonth(int m) {
		if (m <= 9) {
			return "0" + m;
		}
		return "" + m;
	}

	public boolean isOpen(int date) {
		Optional<TradeCal> opt = calDao.findById(date);
		if (opt.isPresent()) {
			return opt.get().getIs_open() == 1;
		}
		return false;
	}

	public String getPretradeDate(String date) {
		return "" + getPretradeDate(Integer.valueOf(date));
	}

	public int getNextDate(int d) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("cal_date").gt(d));
		bqb.must(QueryBuilders.matchPhraseQuery("is_open", 1));
		FieldSortBuilder sort = SortBuilders.fieldSort("cal_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		NativeSearchQueryBuilder builder = queryBuilder.withQuery(bqb);
		Pageable pageable = PageRequest.of(0, 1);
		builder.withPageable(pageable).withSort(sort);
		SearchQuery sq = builder.build();
		Page<TradeCal> page = calDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0).getCal_date();
		}
		throw new RuntimeException("未找到" + d + "的下一个交易日");
	}

	public int getPretradeDate(int d) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("cal_date").lt(d));
		bqb.must(QueryBuilders.matchPhraseQuery("is_open", 1));
		FieldSortBuilder sort = SortBuilders.fieldSort("cal_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		NativeSearchQueryBuilder builder = queryBuilder.withQuery(bqb);
		Pageable pageable = PageRequest.of(0, 1);
		builder.withPageable(pageable).withSort(sort);
		SearchQuery sq = builder.build();
		Page<TradeCal> page = calDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0).getCal_date();
		}
		throw new RuntimeException("未找到" + d + "的上一个交易日");
	}

//	@javax.annotation.PostConstruct
	public void test() {
		JSONObject result = HttpUtil.doGet(String.format(url, "2023-05", Math.random()));
		JSONArray data = (JSONArray) result.get("data");

		for (int i = 0; i < data.size(); i++) {
			JSONObject row = data.getJSONObject(i);
			TradeCal tc = new TradeCal();
			tc.setCal_date(DateUtil.convertDate2(row.getString("jyrq")));// 日期
			System.err.println(tc.getCal_date() + " -> " + getPretradeDate(tc.getCal_date()));
		}
		System.exit(0);
	}
}
