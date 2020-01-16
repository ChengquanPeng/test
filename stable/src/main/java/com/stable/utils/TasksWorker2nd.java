package com.stable.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class TasksWorker2nd {
	public static final int WORKS_NUM = 20;
	public static final Semaphore semp = new Semaphore(WORKS_NUM);
	private static ListeningExecutorService service = MoreExecutors
			.listeningDecorator(Executors.newFixedThreadPool(WORKS_NUM));

	public static void add(MyRunnable task) throws Exception {
		semp.acquire();
		service.submit(task);
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
						Thread.sleep(1 * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			index++;
			if (index % 10 == 0) {
				System.err.println("Index:" + index);
			}
		}
	}
}
