package com.stable.utils;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;

public class TasksWorker {
	public static final int WORKS_NUM = 50;
	private static TasksWorker instance = new TasksWorker();

	private ListeningExecutorService service = MoreExecutors
			.listeningDecorator(Executors.newFixedThreadPool(WORKS_NUM));

	public ListeningExecutorService getService() {
		return this.service;
	}

	public static TasksWorker getInstance() {
		return instance;
	}
}
