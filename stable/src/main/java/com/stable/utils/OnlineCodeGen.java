package com.stable.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.stable.service.model.RunModelService;

public class OnlineCodeGen {
	private static final long WAIT_MIN = 30 * 60 * 1000;// 30MIN
	Map<String, String> warningCode = new ConcurrentHashMap<String, String>();
	RunModelService runModelService;
	public boolean isRunning = true;

	public OnlineCodeGen(RunModelService runModelService) {
		this.runModelService = runModelService;

		Runnable rtt2 = new Runnable() {
			public void run() {
				while (isRunning) {
					try {
						Thread.sleep(WAIT_MIN);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					runModelService.printOnlineHtml(warningCode);
				}
			}
		};
		new Thread(rtt2).start();
	}

	public void genMsg(String code, String title) {
		warningCode.put(code, title);
	}

	public void stop() {
		isRunning = false;
	}
}