package com.stable.service.model.data;

import java.util.List;

import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;

public class LineVol {

	private ModelContext cxt;
	private List<DaliyBasicInfo> dailyList;

	public LineVol(ModelContext cxt, List<DaliyBasicInfo> dailyList) {
		this.cxt = cxt;
		this.dailyList = dailyList;
	}

	// 换手率超过30%或者低于2%
	public boolean isHighOrLowVolToday() {
		double rate = dailyList.get(0).getTurnover_rate_f();
		if (rate >= 30.0) {
			cxt.setDropOutMsg("换手率超过30%");
			return true;
		} else if (rate <= 2.0) {
			cxt.setDropOutMsg("换手率低于2%");
			return true;
		}
		return false;
	}

	public boolean moreVolWithAvg() {
		DaliyBasicInfo d3 = dailyList.get(0);
		DaliyBasicInfo d2 = dailyList.get(1);
		DaliyBasicInfo d1 = dailyList.get(2);

		// ======= 短线--交易量指标 =======
		long total = 0;
		for (int i = 0; i < 30; i++) {
			total += dailyList.get(i).getVol();
		}
		long avgVol = Double.valueOf((total / 30) * 1.2).longValue();
		// 2天连续放量上涨
		if (d3.getVol() > avgVol && d3.getVol() > d2.getVol() && d2.getVol() > d1.getVol()
				&& d3.getClose() > d2.getClose() && d2.getClose() > d1.getClose()) {
			cxt.addDetailDesc("2天连续放量上涨-avgVol");
			return true;
		}
		// 突然放量上涨
		if (d3.getClose() > d2.getClose() && d3.getVol() > d2.getVol()) {
			long half = d2.getVol() / 2;
			if (d3.getVol() >= (d2.getVol() + half) && d3.getVol() > avgVol) {
				cxt.addDetailDesc("突然放量上涨");
				return true;
			}
		}
		return false;
	}

	public boolean moreVol() {
		DaliyBasicInfo d3 = dailyList.get(0);
		DaliyBasicInfo d2 = dailyList.get(1);
		DaliyBasicInfo d1 = dailyList.get(2);

		// 2天连续放量上涨
		if (d3.getVol() > d2.getVol() && d2.getVol() > d1.getVol() && d3.getClose() > d2.getClose()
				&& d2.getClose() > d1.getClose()) {
			cxt.addDetailDesc("2天连续放量上涨");
			return true;
		}
		// 突然放量上涨
		if (d3.getClose() > d2.getClose() && d3.getVol() > d2.getVol()) {
			long half = d2.getVol() / 2;
			if (d3.getVol() >= (d2.getVol() + half)) {
				cxt.addDetailDesc("突然放量上涨");
				return true;
			}
		}
		return false;
	}
}
