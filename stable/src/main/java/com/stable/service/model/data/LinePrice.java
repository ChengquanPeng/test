package com.stable.service.model.data;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.stable.utils.CurrencyUitl;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;

import lombok.Getter;
import lombok.Setter;

public class LinePrice {

	private ModelContext cxt;
	private List<DaliyBasicInfo> dailyList;
	private StockAvg todayAv;
	private StrongService strongService;
	private int lastDate;
	private DaliyBasicInfo today;

	public LinePrice(StrongService strongService, ModelContext cxt, List<DaliyBasicInfo> dailyList, StockAvg todayAv,
			int lastDate) {
		this.cxt = cxt;
		this.dailyList = dailyList;
		this.todayAv = todayAv;
		this.strongService = strongService;
		this.lastDate = lastDate;
		today = cxt.getToday();
	}

	@Getter
	@Setter
	public class StrongResult {
		int strongScore;
		String strongDetail;
	}

	StrongResult sr = null;

	// 涨跌对比大盘
	public StrongResult strongScore() {
		if (sr != null) {
			return sr;
		}
		int sortStrong = 0;
		Map<Integer, Double> cache = strongService.getIndexMap(cxt.getCode(), cxt.getDate(), lastDate);
		int index = 5;
		double base = 0d;
		double stock = 0d;
		int days = 0;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = dailyList.get(i);
			double stdTodayChangeRate = cache.get(db.getTrade_date());
			if (db.getTodayChangeRate() > stdTodayChangeRate) {
				days++;
				if (db.getTodayChangeRate() > 0 && stdTodayChangeRate < 0) {
					// 2，大盘下跌时，个股强势基础分+1，个股翻红时：大盘下跌0-0.5%以内+2，大盘下跌0.5%-1%以内+3，大盘下跌1-2%之间+5，大盘下跌2%以上+7，
					if (stdTodayChangeRate >= -0.5) {
						sortStrong += 2;
					} else if (stdTodayChangeRate >= -1.0) {
						sortStrong += 3;
					} else if (stdTodayChangeRate >= -2.0) {
						sortStrong += 5;
					} else {
						sortStrong += 7;
					}
				}
			}
			stock += db.getTodayChangeRate();
			base += cache.get(db.getTrade_date());
		}
		sr = new StrongResult();
		if (sortStrong > 0) {
			if (stock > base) {
				sortStrong++;
				sortStrong += 5;// 提高权重
				sr.setStrongDetail("5天对比大盘强势次数:" + days + "days[" + sortStrong + "]");
				sr.setStrongScore(sortStrong);
			} else {
				sr.setStrongScore(0);
			}
		}
		return sr;
	}

	private boolean isHignOpenWithLowCloseTodayGet = false;
	private boolean isHignOpenWithLowCloseTodayRes = false;

	// 排除高开低走
	public boolean isHignOpenWithLowCloseToday() {
		if (isHignOpenWithLowCloseTodayGet) {
			return isHignOpenWithLowCloseTodayRes;
		}
		if (todayAv != null && today.getClose() > todayAv.getAvgPriceIndex5()) {
			// 不管涨跌，收盘在5日线上
			isHignOpenWithLowCloseTodayRes = false;
			isHignOpenWithLowCloseTodayGet = true;
			return isHignOpenWithLowCloseTodayRes;
		}
		// 开盘高于昨收，收盘低于开盘
		if (today.getOpen() > today.getYesterdayPrice() && today.getOpen() > today.getClose()) {
			isHignOpenWithLowCloseTodayRes = true;
		}
		isHignOpenWithLowCloseTodayGet = true;
		return isHignOpenWithLowCloseTodayRes;
	}

	private boolean isHighOrLowVolTodaysGet = false;
	private boolean isHighOrLowVolTodayRes = false;

	/**
	 * 至少涨3%
	 */
	public boolean isUp3percent() {
		return today.getTodayChangeRate() > 3.0;
	}

	// 排除上影线(上涨情况下：收盘>昨收=(最高-昨收)/2,下跌排除TODO)
	public boolean isHighOrLowVolToday() {
		if (isHighOrLowVolTodaysGet) {
			return isHighOrLowVolTodayRes;
		}
		if (today.getTodayChange() > 0) {
			double diff = today.getHigh() - today.getYesterdayPrice();
			double half = diff / 2;
			double mid = CurrencyUitl.roundHalfUp(half) + today.getYesterdayPrice();
			if (mid >= today.getClose()) {
				isHighOrLowVolTodayRes = true;
			}
		}
		isHighOrLowVolTodaysGet = true;
		return false;
	}

	private boolean isRange20pWith20daysGet = false;
	private boolean isRange20pWith20daysRes = false;

	// 20天波动超过20%
	public boolean isRange20pWith20days() {
		if (isRange20pWith20daysGet) {
			return isRange20pWith20daysRes;
		}
		List<DaliyBasicInfo> day20 = new LinkedList<DaliyBasicInfo>();
		for (int i = 0; i < 20; i++) {
			day20.add(dailyList.get(i));
		}
		double max20 = day20.stream().max(Comparator.comparingDouble(DaliyBasicInfo::getHigh)).get().getHigh();
		double min20 = day20.stream().min(Comparator.comparingDouble(DaliyBasicInfo::getLow)).get().getLow();
		if (max20 > CurrencyUitl.topPrice20(min20)) {
			isRange20pWith20daysRes = true;
		}
		isRange20pWith20daysGet = true;
		return isRange20pWith20daysRes;
	}

	private boolean isRange30pWith30daysGet = false;
	private boolean isRange30pWith30daysRes = false;

	// 20天波动超过30%
	public boolean isRange30pWith30days() {
		if (isRange30pWith30daysGet) {
			return isRange30pWith30daysRes;
		}
		List<DaliyBasicInfo> day30 = new LinkedList<DaliyBasicInfo>();
		for (int i = 0; i < 30; i++) {
			day30.add(dailyList.get(i));
		}
		double max30 = day30.stream().max(Comparator.comparingDouble(DaliyBasicInfo::getHigh)).get().getHigh();
		double min30 = day30.stream().min(Comparator.comparingDouble(DaliyBasicInfo::getLow)).get().getLow();
		if (max30 > CurrencyUitl.topPrice30(min30)) {
			isRange20pWith20daysRes = true;
		}
		isRange30pWith30daysGet = true;
		return isRange30pWith30daysRes;
	}
}
