package com.stable.utils;

import java.math.BigDecimal;

public class DoubleUtil {

	/**
	 * 四舍五入
	 */
	public static double formatDouble2Bit(double s) {
		BigDecimal b = new BigDecimal(s);
		return b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public static double getRateDouble2Bit(double up, double base) {
		if (up == 0.0) {
			return 0.0;
		}
		BigDecimal b = new BigDecimal(Math.abs(up));
		String s = b.divide(new BigDecimal(Math.abs(base)), 2, BigDecimal.ROUND_HALF_UP)
				.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "";
		if (up < 0.0) {
			s = "-" + s;
		}
		return Double.valueOf(s);
	}

	public static void main(String[] args) {
		System.err.println(DoubleUtil.getRateDouble2Bit(0, 0));
		System.err.println(DoubleUtil.getRateDouble2Bit(17, 100));
		System.err.println(DoubleUtil.getRateDouble2Bit(-17, 100));
	}
}
