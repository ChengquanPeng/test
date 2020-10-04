package com.stable.utils;

public abstract class TasksWorker2ndRunnable implements Runnable {

	@Override
	public void run() {
		try {
			TasksWorker2nd.semp.acquire();
			running();
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "", "", "");
			e.printStackTrace();
		} finally {
			TasksWorker2nd.semp.release();
		}
	}

	public abstract void running();
}
