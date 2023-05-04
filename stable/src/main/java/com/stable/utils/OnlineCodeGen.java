package com.stable.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.stable.service.model.RunModelService;
import com.stable.vo.bus.OnlineMsg;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class OnlineCodeGen {
	private static final long WAIT_MIN = 6 * 60 * 1000;// 6MIN
	public boolean isRunning = true;
	public static boolean x7Chk = false;

	public OnlineCodeGen(RunModelService runModelService) {
		Runnable rtt2 = new Runnable() {
			public void run() {
				try {
					runModelService.printModelHtml();// 启动时重新生成一遍
				} catch (Exception e) {
					e.printStackTrace();
				}
				while (isRunning) {
					try {
						Thread.sleep(WAIT_MIN);
						runModelService.printOnlineHtml(list);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				log.info("OnlineCodeGen end");
			}
		};
		new Thread(rtt2).start();
	}

	int index = 1;
	List<OnlineMsg> list = Collections.synchronizedList(new LinkedList<>());
	Map<String, OnlineMsg> warningCode = new ConcurrentHashMap<String, OnlineMsg>();

	public synchronized void genMsg(String code, String title) {
		OnlineMsg t = warningCode.get(code);
		if (t != null) {
			title = t.getTitle() + " , " + title;
		} else {
			t = new OnlineMsg();
			t.setCode(code);
			t.setIndex(index);
			t.setTitle(title);
			index++;
			list.add(t);
		}
		warningCode.put(code, t);
	}

	public void stop() {
		isRunning = false;
	}
}
