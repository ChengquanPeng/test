package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.spider.ths.ThsSpider;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryWorkingDayJobThsGn extends MySimpleJob {

	@Autowired
	private ThsSpider thsSpider;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("同花顺概念同步");
		thsSpider.start();
	}
}
