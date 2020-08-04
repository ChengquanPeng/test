package com.stable.service.realtime;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.stable.utils.DateUtil;
import com.stable.utils.ScheduledWorker;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsResulter implements Runnable {
	private static final String BR = "</br>";
	private ReentrantLock lock = new ReentrantLock();
	private Map<String, RealtimeMsg> msgs = new ConcurrentHashMap<String, RealtimeMsg>();

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

//String msg = "关注:" + code + " " + codeName + ",市场行为:" + (buytime ? "买入" : "卖出") + ",主力行为:"
	// + (pg ? "Yes" : "No") + ",买入额:" +
	// CurrencyUitl.covertToString(d.getBuyTotalAmt())
	// + ",卖出额:" + CurrencyUitl.covertToString(d.getSellTotalAmt()) + ",总交易额:"
	// + CurrencyUitl.covertToString(d.getTotalAmt()) + ",第一次提醒时间:" +
	// firstTimeWarning
	// + ",提醒次数:" + warningCnt + ",chkPrice:" + chkPrice + ",当前价格:" + nowPrice;
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
				ConcurrentHashMap<String, RealtimeMsg> collect1 = msgs.entrySet().stream()
						.sorted(new Comparator<Map.Entry<String, RealtimeMsg>>() {
							@Override
							public int compare(Map.Entry<String, RealtimeMsg> o1, Map.Entry<String, RealtimeMsg> o2) {
								return Integer.valueOf(o1.getValue().getBaseScore())
										.compareTo(Integer.valueOf(o2.getValue().getBaseScore()));
							}
						}).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
								ConcurrentHashMap::new));

				StringBuffer sb = new StringBuffer("风险第一！！！>>");
				sb.append(BR);
				int index = 1;
				for (String key : collect1.keySet()) {
					RealtimeMsg rm = msgs.get(key);
					if (type == 1) {
						if ((rm.getChkVol1() * 1.3) < rm.getTotalVol() && (rm.getChkVol2() * 1.3) < rm.getTotalVol()) {// 半天的量》均值的半天或者整天的量
							continue;
						}
					} else if (type == 2) {
						if ((rm.getChkVol2() * 1.3) < rm.getTotalVol()) {// 整天的量》均值的半天或者整天的量
							continue;
						}
					}
					sb.append("序号:").append(index).append(",").append(rm.toMessage()).append(BR);
					index++;
				}
				sb.append("请关注量(同花顺)，提防上影线，高开低走等, 链接:http://106.52.95.147:9999/web/realtime/buy?stop?detail?code=");
				WxPushUtil.pushSystem2(sb.toString());
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

		// 开盘一次
		Date d1 = DateUtil.parseDate(today + "094000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d0940 = d1.getTime();
		if (now <= d0940) {
			ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
				@Override
				public void run() {
					sendMsg(0);
				}
			}, d1);
			log.info("scheduled Task with Time:{}", d1);
		}

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
		Date d3 = DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
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
