package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.ThreadsUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 每周执行的任务
 * 
 */
@Component
@Log4j2
public class EveryWeekMonJob extends MySimpleJob {

	@Autowired
	private FinanceService financeService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("新股初始化");
		stockBasicService.jobSynStockListV2Dir();
		ThreadsUtil.sleepRandomSecBetween15And30();

//		log.info("沪深A股，股票池检查");
//		stockSyn.stockListChk();
//		ThreadsUtil.sleepRandomSecBetween15And30();

		log.info("删除退市数据");
		codeModelService.cleanOfflineCode();
		ThreadsUtil.sleepRandomSecBetween15And30();

		log.info("同步股票报告及模型相关");
		financeService.byJob();
		MsgPushServer.pushToSystem("周日任务已完成调用");
	}
}
