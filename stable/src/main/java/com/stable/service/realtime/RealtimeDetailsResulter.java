package com.stable.service.realtime;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
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
			log.info("message size:" + msgs.size());
			if (msgs.size() > 0) {
				StringBuffer sb = new StringBuffer("风险第一！！！>>");
				sb.append(BR);
				int index = 1;
				for (String x : msgs) {
					sb.append("序号:").append(index).append(x);
					index++;
				}
				sb.append("请关注量(同花顺)，提防上影线，高开低走等, 链接:http://106.52.95.147:9999/web/realtime/buy?stop?detail?code=");
				WxPushUtil.pushSystem2(sb.toString());
				msgs = new LinkedList<String>();
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
