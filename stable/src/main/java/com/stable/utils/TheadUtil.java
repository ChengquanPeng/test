package com.stable.utils;

public class TheadUtil {

	
	
	public static void tuShareSleepRandom() {
		sleepRandomSecBetween1And5();
	}
	
	public static void thsSleepRandom() {
		sleepRandomSecBetween1And5();
	}
	
	public static void sinaSleepRandom() {
		sleepRandomSecBetween5And15();
	}
	
	public static void sleepRandomSecBetween1And30() {
		try {
			// 随机休息5-15s
			Thread.sleep(MathUtil.getRandomSecBetween1And30() * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void sleepRandomSecBetween1And5() {
		try {
			// 随机休息5-15s
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
