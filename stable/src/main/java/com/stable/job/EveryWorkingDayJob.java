package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.FinanceService;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryWorkingDayJob extends MySimpleJob {

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private FinanceService financeService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("EveryWorkingDayJob start");
		daliyBasicHistroyService.jobSpiderAllDailyBasic();
		financeService.jobSpiderFirstFinaceHistoryInfo();
		log.info("EveryWorkingDayJob end");
	}
}
