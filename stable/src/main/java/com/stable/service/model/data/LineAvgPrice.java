package com.stable.service.model.data;

import java.util.List;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.service.DaliyTradeHistroyService;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

//@Log4j2
public class LineAvgPrice {
	private final static EsQueryPageReq queryPage30 = new EsQueryPageReq(30);
	// construction
	private AvgService avgService;
	private String code;
	private int lastDate;
	private int date;
	private DaliyBasicInfo today;

	private List<DaliyBasicInfo> dailyList;
	// TEMP
	private List<StockAvg> clist30;
	private List<StockAvg> week4;
	public StockAvg todayAv;
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public LineAvgPrice(AvgService avgService, ModelContext cxt, int lastDate, List<DaliyBasicInfo> dailyList,
			DaliyTradeHistroyService daliyTradeHistroyService) {
		this.avgService = avgService;
		this.code = cxt.getCode();
		this.lastDate = lastDate;
		this.date = cxt.getDate();
		this.dailyList = dailyList;
		this.daliyTradeHistroyService = daliyTradeHistroyService;
		today = cxt.getToday();
	}

	public LineAvgPrice(String code, int date, AvgService avgService, List<DaliyBasicInfo> dailyList) {
		this.code = code;
		this.date = date;
		this.avgService = avgService;
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
		clist30 = avgService.queryListByCodeForModelWithLastQfq(code, date);
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
		int lastDate = dailyList.get(29).getTrade_date();// 第30个
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
				today.getTrade_date(), queryPage30, SortOrder.DESC);
		if (list == null || list.size() < 30) {
			throw new RuntimeException(code + "获取复权数据从" + lastDate + "到" + today.getTrade_date() + "错误！");
		}
		// 复权数据
		for (int i = 0; i < 30; i++) {
			if (list.get(i).getClosed() >= clist30.get(i).getAvgPriceIndex30()) {
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
		if (todayAv.getAvgPriceIndex5() >= todayAv.getAvgPriceIndex10()
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
		if (todayAv.getAvgPriceIndex5() >= todayAv.getAvgPriceIndex20()
				&& todayAv.getAvgPriceIndex10() >= todayAv.getAvgPriceIndex20()
				&& todayAv.getAvgPriceIndex20() >= todayAv.getAvgPriceIndex30()) {
			return true;
		}
		return false;
	}

}
