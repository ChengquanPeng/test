package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;

@Component
public class EveryDayMorningJob extends MySimpleJob {

	@Autowired
	private ThsJobLine thsJobLine;

	@Override
	public void myexecute(ShardingContext sc) {
		thsJobLine.start();
	}
}
