package com.stable.job;

import java.time.Duration;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.constant.RedisConstant;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.utils.MathUtil;
import com.stable.utils.RedisUtil;
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
	@Autowired
	private RedisUtil redisUtil;

	@Override
	public void execute(ShardingContext sc) {
		log.info("每周1任务开始执行：");
		log.info("1.同步股票列表");
		log.info("2.同步股票报告");
		stockBasicService.synStockList();
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : list) {
			String rv = redisUtil.get(RedisConstant.RDS_FINACE_HIST_INFO_ + s.getCode());
			if (StringUtils.isNotBlank(rv)) {
				continue;
			}
			if (financeService.spiderFinaceHistoryInfo(s.getCode())) {
				redisUtil.set(RedisConstant.RDS_FINACE_HIST_INFO_ + s.getCode(), "1", Duration.ofDays(1));
			}
			try {
				// 随机休息5-15s
				Thread.sleep(MathUtil.getRandomSecBetween5And15() * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@PostConstruct
	public void teest() {
		this.execute(null);
	}
}
