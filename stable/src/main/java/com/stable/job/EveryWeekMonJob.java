package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;

import lombok.extern.log4j.Log4j2;

/**
 * 每周执行的任务
 * 
 */
@Component
@Log4j2
public class EveryWeekMonJob implements SimpleJob {

	@Autowired
	private FinanceService financeService;
	@Autowired
	private StockBasicService stockBasicService;

	@Override
	public void execute(ShardingContext sc) {
		log.info("每周1任务开始执行：");
		log.info("1.同步股票列表");
		stockBasicService.jobSynStockList();
		log.info("2.同步股票报告");
		financeService.jobSpiderFinaceHistoryInfo();
	}
}