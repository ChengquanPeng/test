package com.stable.service.model.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LinePrice {
	private final static EsQueryPageReq queryPage30 = new EsQueryPageReq(30);
	private ModelContext cxt;
	private List<DaliyBasicInfo> dailyList;
	private List<TradeHistInfoDaliy> listD30;
	private StockAvg todayAv;
	private StrongService strongService;
	private int date;
	private String code;
	private DaliyBasicInfo today;
	// 除权
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public LinePrice(StrongService strongService, ModelContext cxt, List<DaliyBasicInfo> dailyList, StockAvg todayAv,
			DaliyTradeHistroyService daliyTradeHistroyService) {
		this.daliyTradeHistroyService = daliyTradeHistroyService;
		this.cxt = cxt;
		this.dailyList = dailyList;
		this.todayAv = todayAv;
		this.strongService = strongService;
		today = cxt.getToday();

		code = cxt.getCode();
		listD30 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, today.getTrade_date(), queryPage30,
				SortOrder.DESC);
		if (listD30 == null || listD30.size() < 30) {
			throw new RuntimeException(code + "获取复权数据从0到" + today.getTrade_date() + "错误！");
		}
	}

	public LinePrice(String code, int date, DaliyTradeHistroyService daliyTradeHistroyService) {
		this.code = code;
		this.date = date;
		this.daliyTradeHistroyService = daliyTradeHistroyService;
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
		Map<Integer, Double> cache = strongService.getIndexMap(cxt.getCode(), cxt.getDate());
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
				sr.setStrongDetail("5天对比大盘强势次数:" + days + "天[" + sortStrong + "]");
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

	/**
	 * 至少涨3%
	 */
	public boolean isUp3percent() {
		return today.getTodayChangeRate() > 3.0;
	}

	private boolean isHighOrLowVolTodaysGet = false;
	private boolean isHighOrLowVolTodayRes = false;

	// 排除上影线(上涨情况下：收盘>昨收+(最高-昨收)/2)
	public boolean isLowClosePriceToday() {
		if (isHighOrLowVolTodaysGet) {
			return isHighOrLowVolTodayRes;
		}
		if (today.getTodayChange() > 0) {
			double up = today.getHigh() - today.getYesterdayPrice();
			double half = up / 2;
			double mid = CurrencyUitl.roundHalfUp(half) + today.getYesterdayPrice();
			if (mid >= today.getClose()) {
				isHighOrLowVolTodayRes = true;
			}
		}
		isHighOrLowVolTodaysGet = true;
		return isHighOrLowVolTodayRes;
	}

	// 排除上影线
	public boolean isLowClosePriceToday(double todayChangeRate, double yesterdayPrice, double closedPrice,
			double highPrice) {
		if (todayChangeRate > 0) {
			double diff = highPrice - yesterdayPrice;
			double half = diff * 0.8;
			double mid = CurrencyUitl.roundHalfUp(half) + yesterdayPrice;
			if (mid >= closedPrice) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		// high,yearter
		double yearter = 10.1;
		double up = 11.01 - yearter;
		double half = up * 0.8;
		double mid = CurrencyUitl.roundHalfUp(half) + yearter;
		System.err.println(mid);
	}

	private boolean isRange20pWith20daysGet = false;
	private boolean isRange20pWith20daysRes = false;

	// 20天波动超过20%
	public boolean isRange20pWith20days() {
		if (isRange20pWith20daysGet) {
			return isRange20pWith20daysRes;
		}

		double max20;
		double min20;
		List<TradeHistInfoDaliy> day20 = new LinkedList<TradeHistInfoDaliy>();
		for (int i = 0; i < 20; i++) {
			day20.add(listD30.get(i));
		}
		max20 = day20.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get().getHigh();

		min20 = day20.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get().getLow();
		double min201 = day20.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getYesterdayPrice)).get()
				.getLow();
		double max201 = day20.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getYesterdayPrice)).get()
				.getHigh();

		if (max201 > max20) {
			max20 = max201;
		}
		if (min201 < min20) {
			min20 = min201;
		}

		if (max20 > CurrencyUitl.topPrice20(min20)) {
			isRange20pWith20daysRes = true;
		}
		isRange20pWith20daysGet = true;
		return isRange20pWith20daysRes;
	}

	private boolean isRange30pWith30daysGet = false;
	private boolean isRange30pWith30daysRes = false;

	// 30天波动超过30%
	public boolean isRange30pWith30days() {
		if (isRange30pWith30daysGet) {
			return isRange30pWith30daysRes;
		}

		double max30;
		double min30;
		List<TradeHistInfoDaliy> day30 = new LinkedList<TradeHistInfoDaliy>();
		for (int i = 0; i < 30; i++) {
			day30.add(listD30.get(i));
		}
		max30 = day30.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get().getHigh();
		min30 = day30.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get().getLow();

		double min201 = day30.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getYesterdayPrice)).get()
				.getLow();
		double max201 = day30.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getYesterdayPrice)).get()
				.getHigh();
		if (max201 > max30) {
			max30 = max201;
		}
		if (min201 < min30) {
			min30 = min201;
		}
		if (max30 > CurrencyUitl.topPrice30(min30)) {
			isRange20pWith20daysRes = true;
		}
		isRange30pWith30daysGet = true;
		return isRange30pWith30daysRes;
	}

	// 当日收盘价超过前3日的最高价
	public boolean check3dayPrice() {
		TradeHistInfoDaliy d4 = listD30.get(0);
		TradeHistInfoDaliy d3 = listD30.get(1);
		TradeHistInfoDaliy d2 = listD30.get(2);
		TradeHistInfoDaliy d0 = listD30.get(3);

		if (d4.getClosed() >= d0.getHigh() && d4.getClosed() >= d2.getHigh() && d4.getClosed() >= d3.getHigh()) {
			return true;
		}
		return false;
	}

	// 明日收盘价超过前3日的最高价
	public boolean check3dayPrice(double topPrice) {
		TradeHistInfoDaliy d10 = listD30.get(0);
		TradeHistInfoDaliy d12 = listD30.get(1);
		TradeHistInfoDaliy d13 = listD30.get(2);
		if (topPrice >= d10.getHigh() && topPrice >= d12.getHigh() && topPrice >= d13.getHigh()) {
			return true;
		}
		return false;
	}

	public boolean checkPriceBack6dayWhitTodayV2() {
		listD30 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date, queryPage30, SortOrder.DESC);
		if (listD30 == null || listD30.size() < 30) {
			throw new RuntimeException(code + "获取复权数据从0到" + today.getTrade_date() + "错误！");
		}
		List<TradeHistInfoDaliy> highList = new ArrayList<TradeHistInfoDaliy>();
		highList.add(listD30.get(3));
		highList.add(listD30.get(4));
		highList.add(listD30.get(5));

		List<TradeHistInfoDaliy> lowList = new ArrayList<TradeHistInfoDaliy>();
		lowList.add(listD30.get(0));
		lowList.add(listD30.get(1));
		lowList.add(listD30.get(2));
		double high = highList.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getYesterdayPrice)).get()
				.getHigh();// 低开情况
		double high2 = highList.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get().getHigh();
		double low2 = lowList.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getYesterdayPrice)).get()
				.getLow();// 高开情况
		double low = lowList.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get().getLow();
		if (high2 > high) {
			high = high2;
		}
		if (low2 < low) {
			low = low2;
		}
		return (high > CurrencyUitl.topPrice(low, false));
	}

	// 当日收盘价超过前3日的最高价
	public boolean check3dayPriceV2() {
		TradeHistInfoDaliy d4 = listD30.get(0);
		TradeHistInfoDaliy d3 = listD30.get(1);
		TradeHistInfoDaliy d2 = listD30.get(2);
		TradeHistInfoDaliy d0 = listD30.get(3);

		if (d4.getClosed() >= d0.getHigh() && d4.getClosed() >= d2.getHigh() && d4.getClosed() >= d3.getHigh()) {
			return true;
		}
		return false;
	}

	static EsQueryPageReq queryPage250 = new EsQueryPageReq(250);

	public boolean oneYearCheck(String code, int date) {
		List<TradeHistInfoDaliy> listD250 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				queryPage250, SortOrder.DESC);
		TradeHistInfoDaliy dmax = listD250.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		TradeHistInfoDaliy dmin = listD250.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
		if (dmin.getDate() > dmax.getDate()) {// 先涨-后跌的情况
			return false;
		}
		double maxPrice = dmax.getHigh();
		double minPrice = dmin.getLow();

		if (CurrencyUitl.cutProfit(minPrice, maxPrice) > 65.0) {
			return false;
		} else {
			log.info("code={},checkDate={},maxprice={},maxpriceDate={},mixprice={},maxpriceDate={}", //
					code, date, maxPrice, dmax.getDate(), minPrice, dmin.getDate());
		}
		return true;
	}
}
