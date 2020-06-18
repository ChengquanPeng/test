package com.stable.service.model.data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.stable.utils.CurrencyUitl;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class LineAvgPrice {
	private final static EsQueryPageReq queryPage = new EsQueryPageReq(30);
	// construction
	private AvgService avgService;
	private String code;
	private int lastDate;
	private int date;
	private List<StockAvg> avgSaveList;
	private List<DaliyBasicInfo> dailyList;
	// TEMP
	private List<StockAvg> clist30;
	private List<StockAvg> week4;
	public StockAvg todayAv;

	public LineAvgPrice(AvgService avgService, ModelContext cxt, int lastDate, List<StockAvg> avgSaveList,
			List<DaliyBasicInfo> dailyList) {
		this.avgService = avgService;
		this.code = cxt.getCode();
		this.lastDate = lastDate;
		this.date = cxt.getDate();
		this.avgSaveList = avgSaveList;
		this.dailyList = dailyList;
	}

	private boolean isWeekAvgGet = false;
	private boolean isWeekAvgRes = false;

	// 是否5日均线在30日线上，超过15天
	public boolean isWeek4AvgOk() {
		if (isWeekAvgGet) {
			return isWeekAvgRes;
		}
		week4 = avgService.getWPriceAvg(code, lastDate, date);
		// 排除下跌周期中，收盘不在W均线上
		long count = week4.stream().filter(x -> {
			return x.getAvgPriceIndex20() > x.getAvgPriceIndex30();
		}).count();
		if (count >= 2) {
			isWeekAvgRes = true;
		}
		isWeekAvgGet = true;
		return isWeekAvgRes;
	}

	private boolean isFeedDataGet = false;
	private boolean isFeedDataGetRes = true;

	public boolean feedData() {
		if (isFeedDataGet) {
			return isFeedDataGetRes;
		}
		// 最近30条
		List<StockAvg> avglistLocalTmp = avgService.queryListByCodeForModel(code, date, queryPage);
		Map<Integer, StockAvg> m = new HashMap<Integer, StockAvg>();
		avglistLocalTmp.forEach(x -> {
			m.put(x.getDate(), x);
		});

		// 最近的30条是否和dailyList的30天，时间是否匹配
		List<StockAvg> avglistLocal = new LinkedList<StockAvg>();
		for (int i = 0; i < 30; i++) {
			if (m.containsKey(dailyList.get(i).getTrade_date())) {
				avglistLocal.add(m.get(dailyList.get(i).getTrade_date()));
			}
		}

		// 不匹配， 补全30天
		if (avglistLocal.size() < 30) {
			clist30 = avgService.getDPriceAvg(code, lastDate, date);
			todayAv = clist30.get(0);
			if (clist30.size() < 30) {
				log.warn("数据不全code={},startDate={},enddate={}", code, lastDate, date);
				isFeedDataGetRes = false;
				isFeedDataGet = true;
				return isFeedDataGetRes;
			}
			clist30.forEach(x -> {
				if (!m.containsKey(x.getDate())) {
					avgSaveList.add(x);
				}
			});
		} else {
			clist30 = avglistLocal;
		}
		todayAv = clist30.get(0);
		isFeedDataGet = true;
		return isFeedDataGetRes;
	}

	private boolean is5Don30DhalfGet = false;
	private boolean is5Don30DhalfRes = false;

	// 是否5日均线在30日线上，超过15天
	public boolean is5Don30Dhalf() {
		if (is5Don30DhalfGet) {
			return is5Don30DhalfRes;
		}
		// 排除下跌周期中，刚开始反转的均线
		long count = clist30.stream().filter(x -> {
			return x.getAvgPriceIndex30() >= x.getAvgPriceIndex5();
		}).count();
		if (count >= 15) {
			is5Don30DhalfRes = true;
		}
		is5Don30DhalfGet = true;
		return is5Don30DhalfRes;
	}

	private boolean isWhiteHorseV2Get = false;
	private boolean isWhiteHorseV2Res = false;

	// 类似白马(30个交易日，20日均线一直在30日均线之上（更加宽松比V1）
	public boolean isWhiteHorseV2() {
		if (isWhiteHorseV2Get) {
			return isWhiteHorseV2Res;
		}
		int whiteHorseTmp = 0;
		for (int i = 0; i < 30; i++) {
			if (clist30.get(i).getAvgPriceIndex20() >= clist30.get(i).getAvgPriceIndex30()) {
				whiteHorseTmp++;
			}
		}
		if (whiteHorseTmp >= 25) {
			isWhiteHorseV2Res = true;
		}
		isWhiteHorseV2Get = true;
		return isWhiteHorseV2Res;
	}

	private boolean isWhiteHorseGet = false;
	private boolean isWhiteHorseRes = false;

	// 类似白马
	public boolean isWhiteHorse() {
		if (isWhiteHorseGet) {
			return isWhiteHorseRes;
		}
		int whiteHorseTmp = 0;
		for (int i = 0; i < 30; i++) {
			if (dailyList.get(i).getClose() >= clist30.get(i).getAvgPriceIndex30()) {
				whiteHorseTmp++;
			}
		}
		if (whiteHorseTmp >= 22) {
			isWhiteHorseRes = true;
		}
		isWhiteHorseGet = true;
		return isWhiteHorseRes;
	}

	// 各均线排列整齐
	public boolean isAvgSort3T30() {
		if (todayAv.getAvgPriceIndex3() >= todayAv.getAvgPriceIndex5()
				&& todayAv.getAvgPriceIndex5() >= todayAv.getAvgPriceIndex10()
				&& todayAv.getAvgPriceIndex10() >= todayAv.getAvgPriceIndex20()
				&& todayAv.getAvgPriceIndex20() >= todayAv.getAvgPriceIndex30()) {
			return true;
		}
		return false;
	}

	public boolean isAvgSort20T30Only() {
		// 20日和30日均线>排列就OK
		if (todayAv.getAvgPriceIndex20() >= todayAv.getAvgPriceIndex30()) {
			return true;
		}
		return false;
	}

	public boolean isAvgSort20T30() {
		// 20和30日均线>各均线
		if (todayAv.getAvgPriceIndex3() >= todayAv.getAvgPriceIndex20()
				&& todayAv.getAvgPriceIndex5() >= todayAv.getAvgPriceIndex20()
				&& todayAv.getAvgPriceIndex10() >= todayAv.getAvgPriceIndex20()
				&& todayAv.getAvgPriceIndex20() >= todayAv.getAvgPriceIndex30()) {
			return true;
		}
		return false;
	}

	// 横盘突破:需要看量
	public boolean isRightUp() {
		double maxAvg30 = clist30.stream().max(Comparator.comparingDouble(StockAvg::getAvgPriceIndex30)).get()
				.getAvgPriceIndex30();
		double minAvg30 = clist30.stream().min(Comparator.comparingDouble(StockAvg::getAvgPriceIndex30)).get()
				.getAvgPriceIndex30();

		double maxAvg3 = clist30.stream().max(Comparator.comparingDouble(StockAvg::getAvgPriceIndex3)).get()
				.getAvgPriceIndex3();
		double minAvg3 = clist30.stream().min(Comparator.comparingDouble(StockAvg::getAvgPriceIndex3)).get()
				.getAvgPriceIndex3();

		// 横盘突破:3日均线整幅10%以内， 30日均线振幅在5%以内
		if (CurrencyUitl.topPrice(minAvg3, false) <= maxAvg3 && //
				CurrencyUitl.topPrice(minAvg30, true) <= maxAvg30) {
			return true;
		}
		return false;
	}
}
