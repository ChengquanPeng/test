package com.stable.service.realtime;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.stable.utils.DateUtil;
import com.stable.utils.ScheduledWorker;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsResulter implements Runnable {
	private static final String BR = "</br>";
	private boolean isRunning = true;
	private ReentrantLock lock = new ReentrantLock();
	private Map<String, String> msgs = new ConcurrentHashMap<String, String>();

	public void addSellMessage(String msg) {

	}

	public void removeBuyMessage(String code) {
		lock.lock();
		try {
			msgs.remove(code);
		} finally {
			lock.unlock();
		}
	}

	public void addBuyMessage(String code, String msg) {
		lock.lock();
		try {
			msgs.put(code, msg);
		} finally {
			lock.unlock();
		}
	}

	public void sendMsg() {
		lock.lock();
		try {
			log.info("message size:" + msgs.size());
			if (msgs.size() > 0) {
				StringBuffer sb = new StringBuffer("风险第一！！！>>");
				sb.append(BR);
				int index = 1;
				for (String key : msgs.keySet()) {
					sb.append("序号:").append(index).append(",").append(msgs.get(key)).append(BR);
					index++;
				}
				sb.append("请关注量(同花顺)，提防上影线，高开低走等, 链接:http://106.52.95.147:9999/web/realtime/buy?stop?detail?code=");
				WxPushUtil.pushSystem2(sb.toString());
				msgs = new ConcurrentHashMap<String, String>();
			}
		} finally {
			lock.unlock();
		}
	}

	private long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private long WAIT_MIN = FIVE_MIN;

	@Override
	public void run() {
		String today = DateUtil.getTodayYYYYMMDD();
		long now = new Date().getTime();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				sendMsg();

			}
		};

		// 开盘一次
		Date d1 = DateUtil.parseDate(today + "094000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d0940 = d1.getTime();
		if (now <= d0940) {
			ScheduledWorker.scheduledTimeAndTask(task, d1);
			log.info("scheduled Task with Time:{}", d1);
		}

		// 中午收盘一次
		Date d2 = DateUtil.parseDate(today + "114000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1140 = d2.getTime();
		if (now <= d1140) {
			ScheduledWorker.scheduledTimeAndTask(task, d2);
			log.info("scheduled Task with Time:{}", d2);
		}

		// 下午收盘一次
		Date d3 = DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1450 = d3.getTime();
		if (now <= d1450) {
			ScheduledWorker.scheduledTimeAndTask(task, d3);
			log.info("scheduled Task with Time:{}", d3);
		}

		// 收盘后
		Date d4 = DateUtil.parseDate(today + "150300", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1503 = d4.getTime();
		if (now <= d1503) {
			ScheduledWorker.scheduledTimeAndTask(task, d4);
			log.info("scheduled Task with Time:{}", d4);
		}

	}

	void older() {
		while (isRunning) {
			try {
				sendMsg();
				Thread.sleep(WAIT_MIN);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(WAIT_MIN);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public void stop() {
		isRunning = false;
	}
}
