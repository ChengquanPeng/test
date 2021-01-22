package com.stable.job;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;

public abstract class MySimpleJob implements SimpleJob {

	@Override
	public void execute(ShardingContext shardingContext) {
		try {
			this.myexecute(shardingContext);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "执行job异常", "", "");
			WxPushUtil.pushSystem1("执行job异常:MySimpleJob");
		}
	}

	public abstract void myexecute(ShardingContext shardingContext);
}
