package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.DividendService;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryWorkingDayMorningJob extends MySimpleJob {

	@Autowired
	private DividendService dividendService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("每日分红实施公告任务开始执行：");
		dividendService.jobRespiderDaliyRecords();
	}
}
