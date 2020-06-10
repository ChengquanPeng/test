package com.stable.utils;

import org.apache.commons.lang3.StringUtils;

import com.stable.vo.bus.TickData;

public class TickDataUitl {
	public static final String UN = "N";
	private static final String BUY = "B";
	private static final String SELL = "S";

	public static TickData getDataObjectFromTushare(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		String str = line.trim().substring(1);
		String[] fv = str.split(",");
		TickData td = new TickData();
		td.setTime(fv[0]);
		td.setPrice(Double.valueOf(fv[1]));
//		td.setChange(Double.valueOf(fv[2]));
		td.setVolume(Double.valueOf(fv[3]).longValue());
		td.setAmount(Double.valueOf(fv[4]).longValue());
		td.setType(fv[5]);
		return td;
	}

//	13:48:43,28.10,53,7,2
	public static TickData getDataObjectFromEasymoney(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		String str = line.trim();
		String[] fv = str.split(",");
		TickData td = new TickData();
		td.setTime(fv[0]);
		td.setPrice(Double.valueOf(fv[1]));
		td.setVolume(Double.valueOf(fv[2]).longValue());
		td.setAmount(Double.valueOf(td.getPrice() * 100 * td.getVolume()).longValue());
		td.setDetailNum(Integer.valueOf(fv[3]));
		int t = Integer.valueOf(fv[4]);
		if (t == 1) {
			td.setType(SELL);
		} else if (t == 2) {
			td.setType(BUY);
		} else {
			td.setType(UN);
		}
		return td;
	}
}
