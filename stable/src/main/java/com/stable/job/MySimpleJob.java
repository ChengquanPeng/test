package com.stable.job;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.config.SpringConfig;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class MySimpleJob implements SimpleJob {

	@Override
	public void execute(ShardingContext shardingContext) {
		try {
			if (SpringConfig.isWindows) {
				log.info("Window 不执行任务:" + this.getClass());
				return;
			}
			this.myexecute(shardingContext);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "执行job异常", "", "");
			WxPushUtil.pushSystem1("执行job异常:MySimpleJob");
		}
	}

	public abstract void myexecute(ShardingContext shardingContext);
}
