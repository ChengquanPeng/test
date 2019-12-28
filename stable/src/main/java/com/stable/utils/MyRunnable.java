package com.stable.utils;

public abstract class MyRunnable implements Runnable {

	@Override
	public void run() {
		try {
			running();
		} finally {
			TasksWorker2nd.semp.release();
		}
	}

	public abstract void running();
}
