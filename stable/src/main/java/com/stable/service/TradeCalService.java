package com.stable.service;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;

import lombok.extern.log4j.Log4j2;

/**
 * 交易日历
 * @author roy
 *
 */
@Service
@Log4j2
public class TradeCalService {

	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;

	public void synTradeCal() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("开始同步日历[started]");
				String start_date = DateUtil.getTodayYYYYMMDD();
				String end_date = DateUtil.getLastDayOfYearYYYYMMDD();
				JSONArray array = tushareSpider.getTradeCal(start_date, end_date);
				if (array == null || array.size() <= 0) {
					log.warn("未获取到交易日历");
					return null;
				}
				for (int i = 0; i < array.size(); i++) {
					JSONArray arr = array.getJSONArray(i);
					String cal_date = arr.getString(0);// 日历日期
					String pretrade_date = arr.getString(1);// 上一个交易日
					redisUtil.set(RedisConstant.RDS_TRADE_CAL_ + cal_date, pretrade_date, Duration.ofDays(62));
				}
				log.info("开始同步日历[end]");
				return null;
			}
		});
	}
}