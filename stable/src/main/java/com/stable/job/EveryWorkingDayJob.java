package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryWorkingDayJob extends MySimpleJob {

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private StockBasicService stockBasicService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("EveryWorkingDayJob start");
		log.info("1.同步股票列表");
		stockBasicService.jobSynStockList(true);
		daliyBasicHistroyService.jobSpiderAllDailyBasic();
		financeService.jobSpiderFirstFinaceHistoryInfo();
		log.info("EveryWorkingDayJob end");
	}
}
