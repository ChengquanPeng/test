package com.stable.utils;

public class TheadUtil {

	public static void sleepRandomSecBetween5And15() {
		try {
			// 随机休息5-15s
			Thread.sleep(MathUtil.getRandomSecBetween5And15() * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
