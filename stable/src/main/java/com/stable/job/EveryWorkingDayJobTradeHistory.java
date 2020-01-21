package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.DaliyTradeHistroyService;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryWorkingDayJobTradeHistory implements SimpleJob {

	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;

	@Override
	public void execute(ShardingContext sc) {
		log.info("分红除权重新获取日交易");
		tradeHistroyService.jobSpiderAll();
	}
}
