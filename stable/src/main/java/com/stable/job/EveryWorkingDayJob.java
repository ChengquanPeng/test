package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.TradeCalService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.DateUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EveryWorkingDayJob extends MySimpleJob {

	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private TradeCalService tradeCalService;

	public synchronized void start() {
		line1();
	}

//	@javax.annotation.PostConstruct
//	private void a() {
//		new Thread(new Runnable() {
//			public void run() {
//				start();
//			}
//		}).start();
//	}

	private void line1() {
		log.info("3.流水任务 [started]");
		String today = DateUtil.getTodayYYYYMMDD();
		if (!tradeCalService.isOpen(Integer.valueOf(today))) {
			log.info("非交易日");
			MsgPushServer.pushToSystem(today + " 非交易日 ,Seq1=>Seq5流水任务不会执行");
			return;
		}
		try {
			// 日交易
			log.info("获取日交易(分红除权)");
			int result = tradeHistroyService.spiderTodayDaliyTrade(true, today);
			if (result == 0) {
				MsgPushServer.pushToSystem("异常执行Seq1=>每日交易前复权，不复权，每日指标,日期=" + today + ",数量:0,以后的链条不会被执行");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 短线模型
			// next2SortMode6(today);
		}
		log.info("流水任务 [end]");
	}

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("EveryWorkingDayJob start");
		start();
		log.info("EveryWorkingDayJob end");
	}
}
