package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.spider.eastmoney.RzrqSpider;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryMonthJobOtherTime extends MySimpleJob {
	@Autowired
	private RzrqSpider rzrqSpider;

	public synchronized void myexecute(ShardingContext sc) {
		log.info("每月-融资融券");
		rzrqSpider.byWeb();
	}
}
