package com.stable.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.stable.service.model.RunModelService;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class OnlineCodeGen {
	private static final long WAIT_MIN = 30 * 60 * 1000;// 30MIN
	Map<String, String> warningCode = new ConcurrentHashMap<String, String>();
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
						runModelService.printOnlineHtml(warningCode);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				log.info("OnlineCodeGen end");
			}
		};
		new Thread(rtt2).start();
	}

	public void genMsg(String code, String title) {
		String t = warningCode.get(code);
		if (t != null) {
			title = t + " , " + title;
		}
		warningCode.put(code, title);
	}

	public void stop() {
		isRunning = false;
	}
}
