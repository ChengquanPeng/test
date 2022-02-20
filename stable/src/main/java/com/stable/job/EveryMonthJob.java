package com.stable.job;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.TradeCalService;
import com.stable.spider.eastmoney.EmDzjySpider;
import com.stable.spider.eastmoney.RzrqSpider;
import com.stable.spider.ths.ThsJiejinSpider;
import com.stable.spider.ths.ThsPlateSpider;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryMonthJob extends MySimpleJob {

	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private EmDzjySpider emDzjySpider;
	@Autowired
	private RzrqSpider rzrqSpider;
	@Autowired
	private ThsJiejinSpider thsJiejinSpider;
	@Autowired
	private ThsPlateSpider thsPlateSpider;

	public synchronized void myexecute(ShardingContext sc) {
		log.info("每月-开始同步日历");
		tradeCalService.josSynTradeCal();
		log.info("每月-融资融券");
		rzrqSpider.byWeb(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				log.info("每月-大宗交易");
				emDzjySpider.byWeb();
				return null;
			}
		});
		log.info("每月-同花顺, 解禁");
		thsJiejinSpider.byJob();// 同花顺, 解禁
		log.info("同花顺-亮点，主营 fetchAll=true");
		thsPlateSpider.fetchAll(true);// 同花顺-亮点，主营 多线程
		WxPushUtil.pushSystem1("每月任务EveryMonthJob 已完成调用");
	}
}
