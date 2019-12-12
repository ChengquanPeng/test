package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.BuyBackService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.DividendService;
import com.stable.utils.DateUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryWorkingDayJob implements SimpleJob {

	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private DividendService dividendService;
	@Autowired
	private BuyBackService buyBackService;

	@Override
	public void execute(ShardingContext sc) {
		String today = DateUtil.getTodayYYYYMMDD();
		log.info("日线数据任务开始执行：");
		tradeHistroyService.jobSpiderAll();
		log.info("每日分红实施公告任务开始执行：");
		dividendService.jobSpiderDividendByDate();
		log.info("分红除权重新获取日交易");
		tradeHistroyService.jobSpiderAll();
		log.info("回购公告");
		buyBackService.jobFetchHistEveryDay(today);
	}
}
