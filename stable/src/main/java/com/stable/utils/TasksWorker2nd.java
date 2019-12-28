package com.stable.utils;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class TasksWorker2nd {
	public static final int WORKS_NUM = 20;
	public static final Semaphore semp = new Semaphore(WORKS_NUM);
	private static ListeningExecutorService service = MoreExecutors
			.listeningDecorator(Executors.newFixedThreadPool(WORKS_NUM));

	public static void add(Runnable task) throws Exception {
		semp.acquire();
		service.submit(task);
	}
}
