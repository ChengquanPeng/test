package com.stable.job;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.SortV6Service;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DaliyJobLine {
	@Autowired
	private SortV6Service sortV6Service;
	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TradeCalService tradeCalService;
	
	@PostConstruct
	private void test() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				start();
			}
		}).start();
	}

	public void start() {
		log.info("DaliyJobLine start");
		log.info("1.同步股票列表");
		stockBasicService.jobSynStockList(true);
		log.info("2.同步股票列表");
		financeService.jobSpiderKuaiYuBao();
		line1();
		log.info("EveryWorkingDayJob end");
	}

	private void line1() {
		log.info("3.流水任务 [started]");
		String today = DateUtil.getTodayYYYYMMDD();
		if (!tradeCalService.isOpen(Integer.valueOf(today))) {
			log.info("非交易日");
			WxPushUtil.pushSystem1(today + " 非交易日 ,Seq1=>Seq5流水任务不会执行");
			return;
		}
		try {
			// 日交易
			next1TradeHistroyJob(today);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 短线模型
			next2SortMode6(today);
		}
		log.info("流水任务 [end]");
	}

	private boolean next1TradeHistroyJob(String today) {
		log.info("获取日交易(分红除权)");
		int result = tradeHistroyService.spiderTodayDaliyTrade(true, today);
		if (result != 0) {
			WxPushUtil.pushSystem1("Seq1=>正常执行=>每日交易前复权，不复权，每日指标,日期=" + today + ",数量:" + result);
			return true;
		} else {
			WxPushUtil.pushSystem1("异常执行Seq1=>每日交易前复权，不复权，每日指标,日期=" + today + ",数量:0,以后的链条不会被执行");
			return false;
		}
	}

	private void next2SortMode6(String today) {
		try {
			TimeUnit.MINUTES.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sortV6Service.sortv6(Integer.valueOf(today));
	}
}
