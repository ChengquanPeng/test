package com.stable.job;

import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class TickDataJob extends MySimpleJob {

	// @Autowired
	// private TickDataService tickDataService;

	public void myexecute(ShardingContext sc) {
		log.info("Tick data fetch");
		// log.info("Tick data fetch:" + tickDataService.fetch("", "", "0", false, "",
		// false));
	}
}
