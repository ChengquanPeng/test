package com.stable.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.stable.config.SpringConfig;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TasksWorker2nd {
	public static SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
	public static final int WORKS_NUM = efc.getWorker2Num();
	static {
		log.info("TasksWorker2nd.WORKS_NUM:{}", WORKS_NUM);
	}
	public static final Semaphore semp = new Semaphore(WORKS_NUM);
	private static ListeningExecutorService service = MoreExecutors
			.listeningDecorator(Executors.newFixedThreadPool(WORKS_NUM));

	public static synchronized ListenableFuture<?> add(TasksWorker2ndRunnable task) throws Exception {
		if (getAvailablePermits()) {
			return service.submit(task);
		}
		return null;
	}

	private static boolean getAvailablePermits() {
		if (semp.availablePermits() > 0) {
			return true;
		} else {
			try {
//				System.err.println(Thread.currentThread().getId() + ":waiting");
				TimeUnit.SECONDS.sleep(1);
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
			TasksWorker2nd.add(new TasksWorker2ndRunnable() {

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
