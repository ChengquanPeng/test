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
	private List<StockAvg> avgList;
	private List<DaliyBasicInfo> dailyList;
	// TEMP
	private List<StockAvg> clist30;
	public StockAvg todayAv;

	public LineAvgPrice(AvgService avgService, ModelContext cxt, int lastDate, List<StockAvg> avgList,
			List<DaliyBasicInfo> dailyList) {
		this.avgService = avgService;
		this.code = cxt.getCode();
		this.lastDate = lastDate;
		this.date = cxt.getDate();
		this.avgList = avgList;
		this.dailyList = dailyList;
	}

	private boolean isFeedDataGet = false;
	private boolean isFeedDataGetRes = true;

	public boolean feedData() {
		if (isFeedDataGet) {
			return isFeedDataGetRes;
		}
		List<StockAvg> avglistLocal = avgService.queryListByCodeForModel(code, date, queryPage);
		// 已有的map
		Map<Integer, StockAvg> map = new HashMap<Integer, StockAvg>();
		if (avglistLocal != null && avglistLocal.size() > 0) {
			avglistLocal.stream().forEach(item -> {
				map.put(item.getDate(), item);
			});
		}
		// 补全30天
		clist30 = new LinkedList<StockAvg>();
		for (int i = 0; i < 30; i++) {
			DaliyBasicInfo d = dailyList.get(i);
			if (map.containsKey(d.getTrade_date())) {
				clist30.add(map.get(d.getTrade_date()));
			} else {
				StockAvg r = avgService.getDPriceAvg(code, lastDate, d.getTrade_date(), avgList);
				if (r == null) {
					log.warn("数据不全code={},startDate={},enddate={}", code, lastDate, d.getTrade_date());
					isFeedDataGetRes = false;
				}
				clist30.add(r);
			}
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
