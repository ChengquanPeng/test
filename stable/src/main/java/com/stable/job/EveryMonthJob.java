package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.TradeCalService;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryMonthJob implements SimpleJob {

	@Autowired
	private TradeCalService tradeCalService;

	@Override
	//@PostConstruct
	public void execute(ShardingContext sc) {
		log.info("开始同步日历");
		tradeCalService.synTradeCal();
	}
}
