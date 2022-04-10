package com.stable.service.model.prd;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.stable.service.monitor.RealtimeDetailsAnalyzer;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.Prd1Monitor;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Prd1RealtimeMonitor implements Runnable {
	private boolean isRunning = true;
	private boolean isPushedException = false;
	ExecutorService service = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());

	private List<Prd1Monitor> list;

	public Prd1RealtimeMonitor(List<Prd1Monitor> l) {
		this.list = l;
	}

	public void run() {
		if (list.size() <= 0) {
			log.info("Prd1RealtimeMonitor,无监听列表");
			return;
		}

		String today = DateUtil.getTodayYYYYMMDD();
		long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		Date date = DateUtil.parseDate(today + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(date);
		long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));

		// 等待时间
		long starttime = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 09:30:05").getTime();
		long from1 = new Date().getTime();
		int millis1 = (int) ((starttime - from1));
		if (millis1 > 0) {
			try {
				Thread.sleep(millis1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// 停牌检查
		List<Prd1MoniWorker> tasks = new LinkedList<Prd1MoniWorker>();
		for (Prd1Monitor pm : list) {
			tasks.add(new Prd1MoniWorker(pm));
		}

		while (isRunning) {
			try {
				long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
				if (d1130 <= now && now <= d1300) {
					long from3 = new Date().getTime();
					int millis = (int) ((date.getTime() - from3));
					log.info("Prd1RealtimeMonitor,中场休息");
					if (millis > 0) {
						Thread.sleep(millis);
					}
				}
				for (Prd1MoniWorker w : tasks) {
					if (!w.stopToday) {// 停牌了
						service.submit(w);
					}
				}
				Thread.sleep(RealtimeDetailsAnalyzer.ONE_MIN);
			} catch (Exception e) {
				if (!isPushedException) {
					WxPushUtil.pushSystem1("Prd1RealtimeMonitor 监听异常！");
					isPushedException = true;
					e.printStackTrace();
				}
			}
		}
	}

	public void stop() {
		isRunning = false;
	}
}
