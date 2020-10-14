package com.stable.service.model.data;

import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.vo.bus.TradeHistInfoDaliy;

public class LineVol {
	private List<TradeHistInfoDaliy> dailyList;

	public LineVol(String code, int date, DaliyTradeHistroyService daliyTradeHistroyService) {
		this.dailyList = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date, EsQueryPageUtil.queryPage5,
				SortOrder.DESC);
	}

	// 当日收盘价超过前3日的量，并且突然放量
	public boolean check3dayVol() {
		TradeHistInfoDaliy d4 = dailyList.get(0);
		TradeHistInfoDaliy d3 = dailyList.get(1);
		TradeHistInfoDaliy d2 = dailyList.get(2);
		TradeHistInfoDaliy d1 = dailyList.get(3);

		if (d4.getVolume() > d1.getVolume() && d4.getVolume() > d2.getVolume() && d4.getVolume() > d3.getVolume()) {
			// 突然放量上涨
			return true;
		}
		return false;
	}

	// 今天的量比昨天的量高
	public boolean isShortVolThanYerteryDay() {
		return dailyList.get(0).getVolume() > dailyList.get(1).getVolume();
	}

	public boolean isHighVol() {
		List<TradeHistInfoDaliy> localdailyList = new LinkedList<TradeHistInfoDaliy>();
		localdailyList.add(dailyList.get(0));
		localdailyList.add(dailyList.get(1));
		localdailyList.add(dailyList.get(2));
		localdailyList.add(dailyList.get(3));
		localdailyList.add(dailyList.get(4));
		// 总量
		long total = 0;
		for (TradeHistInfoDaliy d : localdailyList) {
			total += d.getVolume();
		}
		// 均值*基数
		long chkVol = Double.valueOf(total / 5.0 * 1.8).longValue();

		return localdailyList.get(0).getVolume() >= chkVol;
	}

	public boolean isShortVol(double base) {
		List<TradeHistInfoDaliy> localdailyList = new LinkedList<TradeHistInfoDaliy>();
		localdailyList.add(dailyList.get(0));
		localdailyList.add(dailyList.get(1));
		localdailyList.add(dailyList.get(2));
		localdailyList.add(dailyList.get(3));
		localdailyList.add(dailyList.get(4));
		// 总量
		long total = 0;
		for (TradeHistInfoDaliy d : localdailyList) {
			total += d.getVolume();
		}
		// 均值*基数
		long chkVol = Double.valueOf(total / 5.0 * base).longValue();

		return localdailyList.get(0).getVolume() < chkVol;
	}

}
