package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.TradeCalService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.spider.eastmoney.DzjySpider;
import com.stable.spider.ths.ThsJiejinSpider;
import com.stable.spider.ths.ThsPlateSpider;
import com.stable.spider.xq.ZXStockSyn;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryMonthJob extends MySimpleJob {

	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private DzjySpider emDzjySpider;
	@Autowired
	private ThsJiejinSpider thsJiejinSpider;
	@Autowired
	private ThsPlateSpider thsPlateSpider;
	@Autowired
	private ZXStockSyn zxStockSyn;

	public synchronized void myexecute(ShardingContext sc) {
		log.info("每月-开始同步日历");
		tradeCalService.josSynTradeCal();
		log.info("每月-大宗交易");
		emDzjySpider.byWeb();
		log.info("每月-同花顺, 解禁");
		thsJiejinSpider.byJob();// 同花顺, 解禁
		log.info("同花顺-亮点，主营 fetchAll=true");
		thsPlateSpider.fetchAll(true);// 同花顺-亮点，主营 多线程
		log.info("股票chk");
		zxStockSyn.stockListChk();
		MsgPushServer.pushToSystem("每月任务EveryMonthJob 已完成调用");
	}
}
