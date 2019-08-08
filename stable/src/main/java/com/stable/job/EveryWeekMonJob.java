package com.stable.job;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.utils.MathUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 每周执行的任务
 * 
 */
@Component
@Log4j2
public class EveryWeekMonJob implements SimpleJob {

	@Autowired
	private FinanceService financeService;
	@Autowired
	private StockBasicService stockBasicService;

	@Override
	public void execute(ShardingContext sc) {
		log.info("每周1任务开始执行：");
		log.info("1.同步股票列表");
		log.info("2.同步股票报告");
		stockBasicService.synStockList();
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : list) {
			financeService.spiderFinaceHistoryInfo(s.getCode());
			try {
				// 随机休息5-15s
				Thread.sleep(MathUtil.getRandomSecBetween5And15() * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
