package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;

@Component
public class EveryWorkingDayJob extends MySimpleJob {

	@Autowired
	private DaliyJobLine daliyJobLine;

	@Override
	public void myexecute(ShardingContext sc) {
		daliyJobLine.start();
	}
}
