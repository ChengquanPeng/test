package com.stable.job;

import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;

@Component
public class EveryWorkingDayJob extends MySimpleJob {

	private DaliyJobLine daliyJobLine;

	@Override
	public void myexecute(ShardingContext sc) {
		daliyJobLine.start();
	}
}
