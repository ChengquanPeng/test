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

	public final static String covertToString(double l) {
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

	public static void main(String[] args) {
		System.err.println(CurrencyUitl.covertToLong("1.1万亿"));
		System.err.println(CurrencyUitl.covertToString(997));
		System.err.println(CurrencyUitl.covertToString(-9978890000.0f));
		System.err.println(CurrencyUitl.covertToString(-9970000.0f));
		System.err.println(CurrencyUitl.covertToString(-88010f));
	}
}
