package com.stable.utils;

import com.stable.spider.realtime.RealTime;
import com.stable.vo.bus.MonitorPoolTemp;

//@Log4j2
public class MonitoringUitl {

	// 实时
	public static final boolean isOkForRt(MonitorPoolTemp cp, RealTime srt) {
		return isOk(cp, CurrencyUitl.cutProfit(srt.getYesterday(), srt.getNow()), srt.getHigh(), srt.getLow());
	}

	public static final boolean isOk(MonitorPoolTemp cp, double todayChange, double high, double low) {
//		log.info("cp={},todayChange={},high={},low={}", cp, todayChange, high, low);
		if (cp.getUpTodayChange() > 0 || cp.getDownTodayChange() > 0) {
			if (cp.getUpTodayChange() > 0) {// 涨幅
				if (todayChange >= cp.getUpTodayChange()) {
					return true;
				}
			} else {// 跌幅
				todayChange = Math.abs(todayChange);
				if (todayChange <= cp.getDownTodayChange()) {
					return true;
				}
			}
		}
		if (cp.getUpPrice() > 0) {// 上涨到价格
			if (high >= cp.getUpPrice()) {
				return true;
			}
		}
		if (cp.getDownPrice() > 0) {// 下跌到价格
			if (low > 0 && low <= cp.getDownPrice()) {
				return true;
			}
		}
		return false;
	}

	public static final String okMsg(MonitorPoolTemp cp, RealTime srt) {
		return okMsg(cp, CurrencyUitl.cutProfit(srt.getYesterday(), srt.getNow()), srt.getHigh(), srt.getLow());
	}

	public static final String okMsg(MonitorPoolTemp cp, double todayChange, double high, double low) {
//		log.info("cp={},todayChange={},high={},low={}", cp, todayChange, high, low);
		if (cp.getUpTodayChange() > 0 || cp.getDownTodayChange() > 0) {
			if (cp.getUpTodayChange() > 0) {// 涨幅
				if (todayChange >= cp.getUpTodayChange()) {
					return "涨幅>=" + cp.getUpTodayChange();
				}
			} else {// 跌幅
				todayChange = Math.abs(todayChange);
				if (todayChange <= cp.getDownTodayChange()) {
					return "涨幅<=" + cp.getDownTodayChange();
				}
			}
		}
		if (cp.getUpPrice() > 0) {// 上涨到价格
			if (high >= cp.getUpPrice()) {
				return "股价涨到" + cp.getUpPrice();
			}
		}
		if (cp.getDownPrice() > 0) {// 下跌到价格
			if (low > 0 && low <= cp.getDownPrice()) {
				return "股价跌到" + cp.getDownPrice();
			}
		}
		return "";
	}
}
