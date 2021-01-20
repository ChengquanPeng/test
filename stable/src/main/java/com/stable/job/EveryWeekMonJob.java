package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.BuyBackService;
import com.stable.service.FinanceService;

import lombok.extern.log4j.Log4j2;

/**
 * 每周执行的任务
 * 
 */
@Component
@Log4j2
public class EveryWeekMonJob extends MySimpleJob {

	@Autowired
	private FinanceService financeService;
	@Autowired
	private BuyBackService buyBackService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("每周1任务开始执行：");
		log.info("2.同步股票报告");
		financeService.jobSpiderFinaceHistoryInfo();
		log.info("3.同步回购报告");
		buyBackService.jobFetchHist();
	}
}
