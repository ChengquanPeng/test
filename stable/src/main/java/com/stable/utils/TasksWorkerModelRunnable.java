package com.stable.utils;

public abstract class TasksWorkerModelRunnable implements Runnable {

	@Override
	public void run() {
		try {
			TasksWorkerModel.semp.acquire();
			running();
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "", "", "");
			e.printStackTrace();
		} finally {
			TasksWorkerModel.semp.release();
		}
	}

	public abstract void running();
}
