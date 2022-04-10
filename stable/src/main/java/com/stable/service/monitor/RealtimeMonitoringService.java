package com.stable.service.monitor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.ModelWebService;
import com.stable.service.model.ShotPointCheck;
import com.stable.service.model.prd.Prd1RealtimeMonitor;
import com.stable.service.model.prd.Prd1Service;
import com.stable.spider.tick.TencentTickReal;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.MonitorPool;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RealtimeMonitoringService {

	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;
	private Map<String, RealtimeDetailsAnalyzer> map = null;
	@Autowired
	private ModelWebService modelWebService;
	@Autowired
	private ShotPointCheck shotPointCheck;
	@Autowired
	private Prd1Service prd1Service;

	public synchronized void startObservable() {
		String date = DateUtil.getTodayYYYYMMDD();
		int idate = Integer.valueOf(date);
		if (!tradeCalService.isOpen(idate)) {
			// WxPushUtil.pushSystem1("非交易日结束监听");
			log.info("非交易日结束监听");
			return;
		}
		long now = new Date().getTime();
		long starttime = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 09:15:00").getTime();
		long endtime = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 15:03:00").getTime();
		if (now < starttime || now > endtime) {// 已超时
			log.info("now > isAlivingMillis,已超时");
			return;
		}

		try {
			// 获取监听列表-常规
			List<MonitorPool> allCode = monitorPoolService.getPoolListForMonitor(1, 0);

			List<RealtimeDetailsAnalyzer> list = new LinkedList<RealtimeDetailsAnalyzer>();
			RealtimeDetailsResulter resulter = new RealtimeDetailsResulter();

			int failtt = 0;
			if (allCode.size() > 0) {
				// ====启动监听线程====
				map = new ConcurrentHashMap<String, RealtimeDetailsAnalyzer>();
				for (MonitorPool cp : allCode) {
					String code = cp.getCode();
//					if (cp.getMonitor() == MonitorType.ZengFaAuto.getCode()) {
//						log.info(code + " ZengFaAuto continue");
//						continue;
//					}
//					if (cp.getMonitor() != MonitorType.MANUAL.getCode()) {
//						log.info(code + " not -MANUAL continue");
//						continue;
//					}
					log.info(code);
					RealtimeDetailsAnalyzer task = new RealtimeDetailsAnalyzer();
					int r = task.init(code, cp, resulter, stockBasicService.getCodeName2(code),
							modelWebService.getLastOneByCodeResp(code), shotPointCheck);
					if (r == 1) {
						new Thread(task).start();
						list.add(task);
						map.put(code, task);
					} else {
						if (r < 0) {
							failtt++;
						}
					}
				}

				// ====启动结果线程====
				if (list.size() > 0) {
					new Thread(resulter).start();
				}
			}
			WxPushUtil.pushSystem1(
					"交易日监听实时交易，监听总数:[" + allCode.size() + "],实际总数[" + list.size() + "],监听失败[" + failtt + "]");

			// ====产品1：三五天 => 买点 === 卖点 ====
			TencentTickReal.tradeDate = idate;
			Prd1RealtimeMonitor prd1m = new Prd1RealtimeMonitor(prd1Service.getMonitorList());
			new Thread(prd1m).start();

			long from3 = new Date().getTime();
			int millis = (int) ((endtime - from3));
			if (millis > 0) {
				Thread.sleep(millis);
			}
			// List<BuyTrace> buyedList = new LinkedList<BuyTrace>();
			// List<BuyTrace> selledList = new LinkedList<BuyTrace>();
			// 到点停止所有线程
			for (RealtimeDetailsAnalyzer t : list) {
				t.stop();
//				if (t.getBuyed() != null) {
//					buyedList.add(t.getBuyed());
//				}
//				if (t.getSelled() != null) {
//					selledList.add(t.getSelled());
//				}
				if (t.highPriceGot) {
					MonitorPool mpt = monitorPoolService.getMonitorPool(t.code);
					mpt.setYearHigh1(0.0);
					monitorPoolService.saveOrUpdate(mpt);
				}
			}
			prd1m.stop();
			//TODO
			// OnlineTesting -> 转换持仓量和可卖，今日买归0
			
//			WxPushUtil.pushSystem2Html("交易日结束监听!");
			// sendEndMessaget(buyedList, selledList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			map = null;
		}
	}

	public void stopThread(String code) {
		if (map != null) {
			if (map.containsKey(code)) {
				map.get(code).stop();
			}
		}
	}
}
