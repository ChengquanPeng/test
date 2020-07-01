package com.stable.service.realtime;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.stable.utils.WxPushUtil;

public class RealtimeDetailsResulter implements Runnable {
	private static final String BR = "</br>";
	private boolean isRunning = true;
	private ReentrantLock lock = new ReentrantLock();
	private List<String> msgs = new LinkedList<String>();

	public void addSellMessage(String msg) {

	}

	public void addBuyMessage(String msg) {
		lock.lock();
		try {
			msgs.add(msg + BR);
		} finally {
			lock.unlock();
		}
	}

	public void sendMsg() {
		lock.lock();
		try {
			if (msgs.size() > 0) {
				StringBuffer sb = new StringBuffer("风险第一！！！>>");
				sb.append(BR);
				msgs.forEach(x -> {
					sb.append(x);
				});
				sb.append("请关注量(同花顺)，提防上影线，高开低走等, 链接:http://106.52.95.147:9999/web/realtime/buy?stop?detail?code=");
				WxPushUtil.pushSystem2(sb.toString());
			}
		} finally {
			lock.unlock();
		}
	}

	private long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private long WAIT_MIN = FIVE_MIN;

	@Override
	public void run() {
		while (isRunning) {
			try {

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
