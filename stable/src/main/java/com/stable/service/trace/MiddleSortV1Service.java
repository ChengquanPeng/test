package com.stable.service.trace;

import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.EsHistTraceDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.StrongService;

import lombok.extern.log4j.Log4j2;

/**
 * 基本面趋势票
 *
 */
@Service
@Log4j2
public class MiddleSortV1Service {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private EsHistTraceDao esHistTraceDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private StrongService strongService;
	@Autowired
	private TradeCalService tradeCalService;

	private String FILE_FOLDER = "/my/free/pvhtml/";

	public static final Semaphore sempAll = new Semaphore(1);

	@PostConstruct
	public void test1() {
		new Thread(new Runnable() {
			@Override
			public void run() {

//				testlocal();

//				sortv4("20190101", "20191231");
//				sortv4("20180101", "20181231");
//				sortv4("20170101", "20171231");
//				sortv4("20160101", "20161231");
//				sortv4("20110101", "20111231");
//				sortv4("20120101", "20121231");
//				sortv4("20130101", "20131231");
			}
		}).start();
	}

}
