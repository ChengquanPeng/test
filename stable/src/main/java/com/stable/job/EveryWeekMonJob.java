package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.FinanceService;
import com.stable.service.model.CodeModelService;

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
	private CodeModelService codeModelService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("删除退市数据");
		codeModelService.cleanOfflineCode();
		log.info("同步股票报告及模型相关");
		financeService.byJob();
	}
}
