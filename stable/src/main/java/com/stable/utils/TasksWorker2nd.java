package com.stable.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class TasksWorker2nd {
	public static final int WORKS_NUM = 20;
	public static final Semaphore semp = new Semaphore(WORKS_NUM);
	private static ListeningExecutorService service = MoreExecutors
			.listeningDecorator(Executors.newFixedThreadPool(WORKS_NUM));

	public static void add(MyRunnable task) throws Exception {
		if (getAvailablePermits()) {
			service.submit(task);
		}
	}

	private static boolean getAvailablePermits() {
		if (semp.availablePermits() > 0) {
			return true;
		} else {
			try {
//				System.err.println(Thread.currentThread().getId() + ":waiting");
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return getAvailablePermits();
		}
	}

	public static void main(String[] args) throws Exception {
		int index = 1;
		while (true) {
			final int i = index;
			TasksWorker2nd.add(new MyRunnable() {

				@Override
				public void running() {
					System.err.println(Thread.currentThread().getId() + ":" + (i));
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			index++;
			System.err.println(Thread.currentThread().getId() + ":Index:" + index);
		}
	}
}
