package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.eastmoney.EmDzjySpider;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DaliyJobLine {
	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private EmDzjySpider emDzjySpider;

	public synchronized void start() {
		log.info("DaliyJobLine start");
		log.info("1.同步股票列表");
		stockBasicService.jobSynStockList(true);
		log.info("2.快预报");
		financeService.jobSpiderKuaiYuBao();
		line1();
		log.info("4.大宗交易");
		emDzjySpider.byJob();// TODO-不抓全量
		monitorPoolService.jobDzjyWarning();
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
			// next2SortMode6(today);
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
}
