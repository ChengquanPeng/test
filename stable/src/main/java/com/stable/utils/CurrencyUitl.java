package com.stable.utils;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import com.stable.constant.Constant;

public class CurrencyUitl {

	private static final String YI = "亿";
	private static final BigDecimal YI_N = new BigDecimal(100000000L);
	private static final String WAN = "万";
	private static final BigDecimal WAN_N = new BigDecimal(10000L);
	private static final String YUAN = "元";

	public final static double roundHalfUp(double price) {
		double result = new BigDecimal(new Double(price).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static Long covertToLong(String str) {
		if (StringUtils.isBlank(str) || str.contains(Constant.NULL) || str.contains(Constant.FALSE)) {
			return 0l;
		}
		// System.err.println("STR:" + str);
		str = str.trim().replace(YUAN, Constant.EMPTY_STRING);
		BigDecimal s = new BigDecimal(1);
		if (str.contains(YI)) {
			str = str.split(YI)[0];
			s = s.multiply(YI_N);
		}
		if (str.contains(WAN)) {
			str = str.split(WAN)[0];
			s = s.multiply(WAN_N);
		}
		BigDecimal b = new BigDecimal(str);
		return Long.valueOf(b.multiply(s).toBigInteger().toString());
	}

	public final static String covertToString(Long l) {

		if (l > 100000000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + YI;
		} else if (l > 10000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(WAN_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + WAN;
		}
		return l.toString();
	}

	public final static String covertToString(Double l) {
		if (l == null) {
			return "0";
		}
		if (l > 100000000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + YI;
		} else if (l > 10000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(WAN_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + WAN;
		} else if (l >= 0) {

		} else if (-100000000 > l) {
			BigDecimal b = new BigDecimal(Math.abs(l));
			return "-" + b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + YI;
		} else if (-10000 > l) {
			BigDecimal b = new BigDecimal(Math.abs(l));
			return "-" + b.divide(WAN_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + WAN;
		}
		return String.valueOf(l);
	}

	// 3%
	public final static double topPrice3p(double preClosedPrice) {
		preClosedPrice = preClosedPrice * 1.03;
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	/**
	 * 涨停价格3
	 */
	public final static double topPrice(double preClosedPrice, boolean isST) {
		if (isST) {
			preClosedPrice = preClosedPrice * 1.05;
		} else {
			preClosedPrice = preClosedPrice * 1.1;
		}
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	/**
	 * 跌停价格
	 */
	public final static double lowestPrice(double preClosedPrice, boolean isST) {
		if (isST) {
			preClosedPrice = preClosedPrice - preClosedPrice * 0.05;
		} else {
			preClosedPrice = preClosedPrice - preClosedPrice * 0.1;
		}
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static double topPrice20(double preClosedPrice) {
		preClosedPrice = preClosedPrice * 1.2;
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static double topPrice30(double preClosedPrice) {
		preClosedPrice = preClosedPrice * 1.3;
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static double topPrice50(double preClosedPrice) {
		preClosedPrice = preClosedPrice * 1.5;
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static double lowestPrice20(double preClosedPrice) {
		preClosedPrice = preClosedPrice - preClosedPrice * 0.2;
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	/**
	 * 涨停价格3
	 */
	public final static double cutProfit(double buyPrice, double soldPrice) {
		if (soldPrice > buyPrice) {
			double per = ((soldPrice - buyPrice) / buyPrice) * 100;
			return new BigDecimal(new Double(per).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		} else if (soldPrice < buyPrice) {
			double per = ((buyPrice - soldPrice) / buyPrice) * 100;
			return new BigDecimal(new Double(per).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		} else {
			return 0.0;
		}
	}

	public static void main(String[] args) {
		System.err.println((11.0 - 10.0) / 10.0 * 100);
		System.err.println(CurrencyUitl.covertToString(253354520l));
//		System.err.println(CurrencyUitl.covertToLong("1.1万亿"));
//		System.err.println(CurrencyUitl.covertToString(997));
//		System.err.println(CurrencyUitl.covertToString(-9978890000.0f));
//		System.err.println(CurrencyUitl.covertToString(-9970000.0f));
//		System.err.println(CurrencyUitl.covertToString(-88010f));
	}
}
