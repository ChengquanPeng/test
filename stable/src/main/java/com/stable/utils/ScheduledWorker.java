package com.stable.utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledWorker {
	// private static final ScheduledExecutorService executorPool =
	// Executors.newScheduledThreadPool(5);

	public static final void scheduledTimeAndTask(TimerTask task, Date time) {
		Timer timer = new Timer();
		timer.schedule(task, time);
	}

	private static final ScheduledExecutorService executorPool = Executors.newScheduledThreadPool(5);

	public static final ScheduledFuture<?> addScheduled(Runnable command, long time, TimeUnit unit) {
		return executorPool.schedule(command, time, unit);
	}
}
