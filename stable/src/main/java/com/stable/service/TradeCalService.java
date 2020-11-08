package com.stable.service;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsTradeCalDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
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
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsTradeCalDao calDao;

//	@PostConstruct
	public void josSynTradeCal() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.TRADE_CAL, RunCycleEnum.MONTH) {
			public Object mycall() {
				log.info("开始同步日历[started]");
				String start_date = DateUtil.getTodayYYYYMMDD();
				String end_date = DateUtil.getLastDayOfYearYYYYMMDD();
				fetchCal(start_date, end_date);
				log.info("开始同步日历[end]");
				return null;
			}
		});
	}

	public void josSynTradeCal2(String start_date, String end_date) {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.TRADE_CAL, RunCycleEnum.MANUAL,
				"start_date:" + start_date + ",end_date:" + end_date) {
			public Object mycall() {
				log.info("开始同步日历[started]");
				fetchCal(start_date, end_date);
				log.info("开始同步日历[end]");
				return null;
			}
		});
	}

	// 整年的日历
	private synchronized void fetchCal(String date) {
		int year = DateUtil.getYear(Integer.valueOf(date));
		fetchCal(year + "0101", year + "1231");
	}

	private synchronized void fetchCal(String start_date, String end_date) {
		JSONArray array = tushareSpider.getTradeCal(start_date, end_date);
		int cnt = 0;
		if (array != null && array.size() > 0) {
			List<TradeCal> list = new LinkedList<TradeCal>();
			for (int i = 0; i < array.size(); i++) {
				JSONArray arr = array.getJSONArray(i);
				String cal_date = arr.getString(0);// 日历日期
				String is_open = arr.getString(1);// 是否开市
				String pretrade_date = arr.getString(2);// 上一个交易日
				redisUtil.set(RedisConstant.RDS_TRADE_CAL_ + cal_date, pretrade_date, Duration.ofDays(92));

				TradeCal tc = new TradeCal();
				tc.setCal_date(Integer.valueOf(cal_date));
				tc.setIs_open(Integer.valueOf(is_open));
				tc.setPretrade_date(Integer.valueOf(pretrade_date));
				// calDao.save(tc);
				list.add(tc);
			}
			if (list.size() > 0) {
				calDao.saveAll(list);
				cnt = list.size();
			}
		} else {
			log.warn("未获取到交易日历");
		}
		String msg = start_date + " " + end_date + "获取交易日历条数=" + cnt;
		log.info(msg);
		// WxPushUtil.pushSystem1(msg);
	}

	public boolean isOpen(int date) {
		Optional<TradeCal> opt = calDao.findById(date);
		if (opt.isPresent()) {
			return opt.get().getIs_open() == 1;
		}
		return false;
	}

	public String getPretradeDate(String date) {
		String s = redisUtil.get(RedisConstant.RDS_TRADE_CAL_ + date);
		if (StringUtils.isBlank(s)) {
			fetchCal(date);
			return redisUtil.get(RedisConstant.RDS_TRADE_CAL_ + date);
		}
		return s;
	}

	public int getPretradeDate(int d) {
		String date = d + "";
		String s = redisUtil.get(RedisConstant.RDS_TRADE_CAL_ + date);
		if (StringUtils.isBlank(s)) {
			fetchCal(date);
			return Integer.valueOf(redisUtil.get(RedisConstant.RDS_TRADE_CAL_ + date));
		}
		return Integer.valueOf(s);
	}
}
