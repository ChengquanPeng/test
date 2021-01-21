package com.stable.utils;

import com.stable.spider.sina.SinaRealTime;
import com.stable.vo.bus.MonitorPool;

public class MonitoringUitl {

	// 实时
	public static final boolean isOkForRt(MonitorPool cp, SinaRealTime srt) {
		return isOk(cp, CurrencyUitl.cutProfit(srt.getYesterday(), srt.getNow()), srt.getHigh(), srt.getLow());
	}

	public static final boolean isOk(MonitorPool cp, double todayChange, double high, double low) {
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
		} else if (cp.getUpPrice() > 0) {// 上涨到价格
			if (high >= cp.getUpPrice()) {
				return true;
			}
		} else if (cp.getDownPrice() > 0) {// 下跌到价格
			if (low <= cp.getDownPrice()) {
				return true;
			}
		}
		return false;
	}
}
