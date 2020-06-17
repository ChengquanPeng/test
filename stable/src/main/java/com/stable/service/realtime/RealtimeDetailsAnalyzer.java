package com.stable.service.realtime;

import com.stable.vo.up.strategy.ModelV1;

public class RealtimeDetailsAnalyzer implements Runnable {
	private long ONE_MIN = 1 * 60 * 1000;// 1MIN
	private ModelV1 modelV1;

	private boolean isRunning = true;

	public void stop() {
		isRunning = false;
	}

	public RealtimeDetailsAnalyzer(ModelV1 modelV1) {
		this.modelV1 = modelV1;
	}

	public void run() {
		try {
			Thread.sleep(ONE_MIN);// 1分钟

			while (isRunning) {

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
