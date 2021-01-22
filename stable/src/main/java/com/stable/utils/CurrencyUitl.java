package com.stable.utils;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import com.stable.constant.Constant;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CurrencyUitl {

	private static final String YI = "亿";
	public static final BigDecimal YI_N = new BigDecimal(100000000L);
	private static final String WAN = "万";
	private static final BigDecimal WAN_N = new BigDecimal(10000L);
	private static final String YUAN = "元";

	public final static double roundHalfUp(double price) {
		double result = new BigDecimal(new Double(price).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static double roundHalfUpWhithPercent(double price) {
		return new BigDecimal(new Double(price * 100).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
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
		if (l == null) {
			return "";
		}
		if (l > 100000000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + YI;
		} else if (l > 10000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(WAN_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + WAN;
		} else if (l < -100000000l) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + YI;
		} else if (l < -10000l) {
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

	public final static double covertToString2(Double l) {
		if (l == null) {
			return 0;
		}
		if (l > 100000000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		} else if (l > 10000) {
			BigDecimal b = new BigDecimal(l);
			return b.divide(WAN_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		} else if (l >= 0) {

		} else if (-100000000 > l) {
			BigDecimal b = new BigDecimal(Math.abs(l));
			return Double.valueOf("-" + b.divide(YI_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		} else if (-10000 > l) {
			BigDecimal b = new BigDecimal(Math.abs(l));
			return Double.valueOf("-" + b.divide(WAN_N).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
		return l;
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
			return topPriceN(preClosedPrice, 1.05);
		} else {
			return topPriceN(preClosedPrice, 1.105);
		}
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
		return topPriceN(preClosedPrice, 1.2);
	}

	public final static double topPrice30(double preClosedPrice) {
		return topPriceN(preClosedPrice, 1.3);
	}

	public final static double topPrice50(double preClosedPrice) {
		return topPriceN(preClosedPrice, 1.5);
	}

	public final static double topPriceN(double preClosedPrice, double rate) {
		preClosedPrice = preClosedPrice * rate;
		double result = new BigDecimal(new Double(preClosedPrice).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		return result;
	}

	public final static double lowestPrice18(double preClosedPrice) {
		preClosedPrice = preClosedPrice - preClosedPrice * 0.18;
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
		try {
			if (buyPrice == 0.0) {
				return 0.0;
			} else if (soldPrice > buyPrice) {
				double per = ((soldPrice - buyPrice) / buyPrice) * 100;
				return new BigDecimal(new Double(per).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			} else if (soldPrice < buyPrice) {
				double per = ((buyPrice - soldPrice) / buyPrice) * 100;
				per = 0.0 - per;
				return new BigDecimal(new Double(per).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			} else {
				return 0.0;
			}
		} catch (Exception e) {
			log.info("buyPrice={},soldPrice={}", buyPrice, soldPrice);
			throw new RuntimeException(e);
		}
	}

	public final static double getRate(int yes, int total) {
		if (total == 0) {
			return 0.0;
		}
		double per = (Double.valueOf(yes) / Double.valueOf(total)) * 100;
		return new BigDecimal(new Double(per).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public static void main(String[] args) {
		// System.err.println(CurrencyUitl.covertToLong("-10亿"));
		// System.err.println(CurrencyUitl.covertToLong("10亿"));
//		double buyPrice = 0.0;
//		double soldPrice = 2.1;
//		System.err.println((0.0 - 2.1));
//		System.err.println((soldPrice - buyPrice));
//		System.err.println((soldPrice - buyPrice) / buyPrice);
//		double per = ((soldPrice - buyPrice) / buyPrice) * 100;
//		System.err.println(per);
//		System.err.println(
//				new BigDecimal(new Double(per).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		System.err.println(CurrencyUitl.covertToLong("4082万"));
//		System.err.println(CurrencyUitl.covertToLong("1.1万亿"));
//		System.err.println(CurrencyUitl.covertToString(997));
//		System.err.println(CurrencyUitl.covertToString(-9978890000.0f));
//		System.err.println(CurrencyUitl.covertToString(-9970000.0f));
//		System.err.println(CurrencyUitl.covertToString(-88010f));
	}
}
