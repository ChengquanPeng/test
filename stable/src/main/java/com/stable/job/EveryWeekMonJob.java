package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.BuyBackService;
import com.stable.service.DividendService;
import com.stable.service.FinanceService;
import com.stable.service.ShareFloatService;
import com.stable.service.StockBasicService;

import lombok.extern.log4j.Log4j2;

/**
 * 每周执行的任务
 * 
 */
@Component
@Log4j2
public class EveryWeekMonJob extends MySimpleJob {

	private static final int TEN_MIN = 10 * 60 * 1000;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private BuyBackService buyBackService;
	@Autowired
	private DividendService dividendService;
	@Autowired
	private ShareFloatService shareFloatService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("每周1任务开始执行：");
		log.info("1.同步股票列表");
		stockBasicService.jobSynStockList(true);
		try {
			Thread.sleep(TEN_MIN);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		log.info("2.同步股票报告");
		financeService.jobSpiderFinaceHistoryInfo();
		log.info("3.同步回购报告");
		buyBackService.jobFetchHist();
		log.info("4.分红送股除权信息同步");
		dividendService.jobSpiderDividendByWeek();
		log.info("5.限售解禁");
		shareFloatService.jobFetchHist();
	}
}
