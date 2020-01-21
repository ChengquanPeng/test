package com.stable.job;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;

public abstract class MySimpleJob implements SimpleJob {

	@Override
	public void execute(ShardingContext shardingContext) {
		this.myexecute(shardingContext);
	}

	public abstract void myexecute(ShardingContext shardingContext);
}
