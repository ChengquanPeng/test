package com.stable.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.stable.config.SpringConfig;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TasksWorkerPrd1 {
	public static SpringConfig efc = null;
	public static int WORKS_NUM = 0;
	public static final int CORE_NUM = 5;
	public static final int QUEUE_NUM = 1000;
	static {
		if (SpringUtil.getApplicationContext() != null) {
			efc = SpringUtil.getBean(SpringConfig.class);
			WORKS_NUM = efc.getWorker2Num();
		}
		if (CORE_NUM >= WORKS_NUM) {
			WORKS_NUM = CORE_NUM;
		}
		log.info("TasksWorkerPrd1.CORE_NUM:{},WORKS_NUM:{},QUEUE_NUM:{}", CORE_NUM, WORKS_NUM, QUEUE_NUM);
	}
	private static ExecutorService service = new ThreadPoolExecutor(CORE_NUM, WORKS_NUM, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());

	public static synchronized void add(Runnable task) throws Exception {
		service.submit(task);
	}

//	public static void main(String[] args) throws Exception {
//		AtomicInteger cntAdd = new AtomicInteger();
//		int index = 1;
//		while (true) {
//			TasksWorkerPrd1.add(new Runnable() {
//				public void run() {
//					// System.err.println(Thread.currentThread().getId() + ":" + (i));
//					cntAdd.decrementAndGet();
//					try {
//						TimeUnit.MILLISECONDS.sleep(10);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			});
//			index++;
//			// System.err.println(Thread.currentThread().getId() + ":Index:" + index);
//			int r = cntAdd.incrementAndGet();
//			if (r >= 6000) {
//				System.err.println(Thread.currentThread().getId() + ":Done:" + index);
//				break;
//			}
//		}
//		while (true) {
//			int a = cntAdd.get();
//			if (a == 0) {
//				break;
//			} else {
//				System.err.println("a====>" + a);
//				TimeUnit.SECONDS.sleep(1);
//			}
//		}
//		System.err.println("ALL Done!");
//	}
}
