package com.stable.service.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
	private ReentrantLock lock = new ReentrantLock();
	private Map<String, RealtimeMsg> msgs = new ConcurrentHashMap<String, RealtimeMsg>();

	public void addBuyMessage(String code, RealtimeMsg msg) {
		lock.lock();
		try {
			msgs.put(code, msg);
		} finally {
			lock.unlock();
		}
	}

	public void sendMsg(int type) {
		lock.lock();
		try {
			log.info("message size:" + msgs.size());
			if (msgs.size() > 0) {
				// 按照基本分排序
				List<RealtimeMsg> list = new ArrayList<RealtimeMsg>();
				for (String key : msgs.keySet()) {
					list.add(msgs.get(key));
				}
				StringBuffer sb = new StringBuffer("风险第一！！！>>");
				sb.append(BR);
				int index = 1;
				for (RealtimeMsg rm : list) {
					sb.append("序号:").append(index).append(",").append(rm.toMessage()).append(BR);
					index++;
				}
				sb.append("股票池+3.5%预警");
				WxPushUtil.pushSystem2Html(sb.toString());
				msgs = new ConcurrentHashMap<String, RealtimeMsg>();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void run() {
		String today = DateUtil.getTodayYYYYMMDD();
		long now = new Date().getTime();

//		// 开盘一次
//		Date d1 = DateUtil.parseDate(today + "094000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
//		long d0940 = d1.getTime();
//		if (now <= d0940) {
//			ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
//				@Override
//				public void run() {
//					sendMsg(0);
//				}
//			}, d1);
//			log.info("scheduled Task with Time:{}", d1);
//		}

		// 中午收盘一次
		Date d2 = DateUtil.parseDate(today + "114000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1140 = d2.getTime();
		if (now <= d1140) {
			ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
				@Override
				public void run() {
					sendMsg(1);
				}
			}, d2);
			log.info("scheduled Task with Time:{}", d2);
		}

		// 下午收盘一次
		Date d3 = DateUtil.parseDate(today + "145300", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1450 = d3.getTime();
		if (now <= d1450) {
			ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
				@Override
				public void run() {
					sendMsg(2);
				}
			}, d3);
			log.info("scheduled Task with Time:{}", d3);
		}

		// 收盘后
//		Date d4 = DateUtil.parseDate(today + "150300", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
//		long d1503 = d4.getTime();
//		if (now <= d1503) {
//			ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
//				@Override
//				public void run() {
//					sendMsg(4);
//				}
//			}, d4);
//			log.info("scheduled Task with Time:{}", d4);
//		}

	}
}
