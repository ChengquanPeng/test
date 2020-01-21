package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.DaliyBasicHistroyService;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryWorkingDayJobTradeBaisc extends MySimpleJob {

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("个股每日指标");
		daliyBasicHistroyService.jobSpiderAllDailyBasic();
	}
}
