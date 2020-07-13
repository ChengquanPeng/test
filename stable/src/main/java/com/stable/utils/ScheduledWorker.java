package com.stable.utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ScheduledWorker {
	// private static final ScheduledExecutorService executorPool =
	// Executors.newScheduledThreadPool(5);

	public static final void scheduledTimeAndTask(TimerTask task, Date time) {
		Timer timer = new Timer();
		timer.schedule(task, time);
	}
}
