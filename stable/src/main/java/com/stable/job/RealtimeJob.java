package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.monitor.RealtimeMonitoringService;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class RealtimeJob extends MySimpleJob {

	@Autowired
	private RealtimeMonitoringService monitoringService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("监听开始");
		monitoringService.startObservable();
	}
}
