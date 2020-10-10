package com.stable.service.model.data;

import java.util.LinkedList;
import java.util.List;

import com.stable.vo.bus.DaliyBasicInfo;

public class LineVol {

	private List<DaliyBasicInfo> dailyList;

	public LineVol(List<DaliyBasicInfo> dailyList) {
		this.dailyList = dailyList;
	}

	public boolean isHighVolToday20Percent() {
		double rate = dailyList.get(0).getTurnover_rate_f();
		if (rate >= 20.0) {
			return true;
		}
		return false;
	}

	// 当日收盘价超过前3日的量，并且突然放量
	public boolean check3dayVol() {
		DaliyBasicInfo d4 = dailyList.get(0);
		DaliyBasicInfo d3 = dailyList.get(1);
		DaliyBasicInfo d2 = dailyList.get(2);
		DaliyBasicInfo d1 = dailyList.get(3);

		if (d4.getVol() > d1.getVol() && d4.getVol() > d2.getVol() && d4.getVol() > d3.getVol()) {
			// 突然放量上涨
			return true;
		}
		return false;
	}

	public boolean isShortVolV2(double base) {
		List<DaliyBasicInfo> localdailyList = new LinkedList<DaliyBasicInfo>();
		localdailyList.add(dailyList.get(0));
		localdailyList.add(dailyList.get(1));
		localdailyList.add(dailyList.get(2));
		localdailyList.add(dailyList.get(3));
		localdailyList.add(dailyList.get(4));
		// 均值x1.3
		long max = Double
				.valueOf(localdailyList.stream().mapToLong(DaliyBasicInfo::getVol).sum() / localdailyList.size() * base)
				.longValue();
		for (DaliyBasicInfo d : localdailyList) {
			if (d.getVol() > max) {
				return false;
			}
		}
		return true;
	}

}
