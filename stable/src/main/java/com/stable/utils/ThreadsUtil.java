package com.stable.utils;

import java.util.concurrent.TimeUnit;

public class ThreadsUtil {

	public static void tuShareSleepRandom() {
		sleepRandomSecBetween1And5();
	}

	public static void thsSleepRandom() {
		sleepRandomSecBetween1And5();
	}

	public static void sinaSleepRandom() {
		sleepRandomSecBetween5And15();
	}

	public static void sleepSleep1Seconds() {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void sleepRandomSecBetween1And2() {
		try {
			// 随机休息1-2s
			if (System.currentTimeMillis() % 2 == 0) {
				TimeUnit.SECONDS.sleep(2);
			} else {
				TimeUnit.SECONDS.sleep(1);
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void sleepRandomSecBetween1And30() {
		try {
			// 随机休息1-30s
			Thread.sleep(MathUtil.getRandomSecBetween1And30() * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void sleepRandomSecBetween1And5() {
		try {
			// 随机休息1-5s
			Thread.sleep(MathUtil.getRandomSecBetween1And5() * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void sleepRandomSecBetween5And15() {
		try {
			// 随机休息5-15s
			Thread.sleep(MathUtil.getRandomSecBetween5And15() * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
