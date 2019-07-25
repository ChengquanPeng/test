package com.stable.job;

import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;

import lombok.extern.slf4j.Slf4j;

/**
 * 这是我们的测试job类
 * 
 * @author
 * @date 2018/10/25.
 */
@Component
@Slf4j
public class TestJob implements SimpleJob {

	@Override
	public void execute(ShardingContext sc) {
		log.info("我是一个定时任务:{},{},{},{},{},{},", sc.getJobName(), sc.getJobParameter(), sc.getShardingParameter(),
				sc.getShardingTotalCount(), sc.getTaskId(),sc.getShardingItem());
	}
}
