package com.stable.service.model.data;

import java.util.List;

import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;

public class LineVol {

	private List<DaliyBasicInfo> dailyList;

	public LineVol(ModelContext cxt, List<DaliyBasicInfo> dailyList) {
		this.dailyList = dailyList;
	}

	// 换手率超过30%或者低于2%
	public int isHighOrLowVolToday() {
		double rate = dailyList.get(0).getTurnover_rate_f();
		if (rate >= 30.0) {
			return 1;
		} else if (rate <= 2.0) {
			return 2;
		}
		return 0;
	}

	public String moreVolWithAvg() {
		DaliyBasicInfo d3 = dailyList.get(0);
		DaliyBasicInfo d2 = dailyList.get(1);
		DaliyBasicInfo d1 = dailyList.get(2);
		String r = "";
		// ======= 短线--交易量指标 =======
		long total = 0;
		for (int i = 0; i < 30; i++) {
			total += dailyList.get(i).getVol();
		}
		long avgVol = Double.valueOf((total / 30) * 1.2).longValue();
		// 2天连续放量上涨
		if (d3.getVol() > avgVol && d3.getVol() > d2.getVol() && d2.getVol() > d1.getVol()
				&& d3.getClose() > d2.getClose() && d2.getClose() > d1.getClose()) {
			r = "2天连续放量上涨-avgVol";
		}
		// 突然放量上涨
		if (d3.getClose() > d2.getClose() && d3.getVol() > d2.getVol()) {
			long half = d2.getVol() / 2;
			if (d3.getVol() >= (d2.getVol() + half) && d3.getVol() > avgVol) {
				r += ",突然放量上涨-avgVol";
			}
		}
		return r;
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

	public String moreVol() {
		DaliyBasicInfo d3 = dailyList.get(0);
		DaliyBasicInfo d2 = dailyList.get(1);
		DaliyBasicInfo d1 = dailyList.get(2);

		String r = "";
		// 2天连续放量上涨
		if (d3.getVol() > d2.getVol() && d2.getVol() > d1.getVol() && d3.getClose() > d2.getClose()
				&& d2.getClose() > d1.getClose()) {
			r = "2天连续放量上涨";
		}
		// 突然放量上涨
		if (d3.getClose() > d2.getClose() && d3.getVol() > d2.getVol()) {
			long half = d2.getVol() / 2;
			if (d3.getVol() >= (d2.getVol() + half)) {
				r += ",突然放量上涨";
			}
		}
		return r;
	}
}
