package com.stable.job;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.TradeCalService;
import com.stable.spider.eastmoney.EmDzjySpider;
import com.stable.spider.eastmoney.RzrqSpider;

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

	public void myexecute(ShardingContext sc) {
		log.info("开始同步日历");
		tradeCalService.josSynTradeCal();
		rzrqSpider.byWeb(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				emDzjySpider.byWeb();
				return null;
			}
		});
	}
}
