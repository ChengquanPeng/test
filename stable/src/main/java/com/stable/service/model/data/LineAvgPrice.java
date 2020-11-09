package com.stable.service.model.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.stable.constant.EsQueryPageUtil;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.spi.req.EsQueryPageReq;

//@Log4j2
public class LineAvgPrice {
	// construction
	private AvgService avgService;

	// TEMP
	public StockAvgBase todayAv;

	public LineAvgPrice(AvgService avgService) {
		this.avgService = avgService;
	}

	private boolean isWhiteHorseV2Get = false;
	private boolean isWhiteHorseV2Res = false;

	// 类似白马(30个交易日，20日均线一直在30日均线之上（更加宽松比V1）
	public boolean isWhiteHorseV2(String code, int date) {
		if (isWhiteHorseV2Get) {
			return isWhiteHorseV2Res;
		}
		// 最近30条
		List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLastAnd30Records(code, date, true);
		todayAv = clist30.get(0);
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

//	public boolean isWhiteHorseV3ForMiddle(String code, int date) {
//		// 最近10条
//		List<StockAvg> clist10 = avgService.queryListByCodeForModelWithLastQfq(code, date, EsQueryPageUtil.queryPage10);
//		// todayAv = clist10.get(0);
//		int whiteHorseTmp = 0;
//		for (int i = 0; i < 10; i++) {
//			if (clist10.get(i).getAvgPriceIndex20() >= clist10.get(i).getAvgPriceIndex30()) {
//				whiteHorseTmp++;
//			}
//		}
//		if (whiteHorseTmp >= 9) {
//			return true;
//		}
//		return false;
//	}

	public boolean isWhiteHorseV3ForMiddleSort(String code, int highDate) {
		// 最近10条
		List<StockAvgBase> clist10 = avgService.queryListByCodeForModelWithLast(code, highDate,
				EsQueryPageUtil.queryPage10, true);
		// todayAv = clist10.get(0);
		int whiteHorseTmp = 0;
		for (int i = 0; i < 3; i++) {
			if (clist10.get(i).getAvgPriceIndex5() >= clist10.get(i).getAvgPriceIndex10()
					&& clist10.get(i).getAvgPriceIndex10() >= clist10.get(i).getAvgPriceIndex20()
					&& clist10.get(i).getAvgPriceIndex20() >= clist10.get(i).getAvgPriceIndex30()) {
				whiteHorseTmp++;
			}
		}
		if (whiteHorseTmp >= 3) {
			return true;
		}
		whiteHorseTmp = 0;
		for (int i = 0; i < 10; i++) {
			if (clist10.get(i).getAvgPriceIndex20() >= clist10.get(i).getAvgPriceIndex30()) {
				whiteHorseTmp++;
			}
		}
		if (whiteHorseTmp >= 9) {
			return true;
		}
		return false;
	}

	public boolean isWhiteHorseForSortV4(String code, int date, boolean isTrace) {
		EsQueryPageReq req = EsQueryPageUtil.queryPage6;
		if (isTrace) {
			req = EsQueryPageUtil.queryPage5;
		}
		// 最近5条
		List<StockAvgBase> clist10 = avgService.queryListByCodeForModelWithLast(code, date, req, false);
		// 前面几天(未企稳)均线最高价和最低价振幅超5%,排除掉
		List<Double> price = new ArrayList<Double>(20);
		for (int i = 0; i < clist10.size(); i++) {
			StockAvgBase sa = clist10.get(i);
			if (isTrace && sa.getDate() == date) {
				continue;
			}
			price.add(sa.getAvgPriceIndex5());
			price.add(sa.getAvgPriceIndex10());
			price.add(sa.getAvgPriceIndex20());
			price.add(sa.getAvgPriceIndex30());
		}
		double max = price.stream().max((p1, p2) -> p1.compareTo(p2)).get();
		double min = price.stream().min((p1, p2) -> p1.compareTo(p2)).get();
		if (CurrencyUitl.cutProfit(min, max) > 5) {
			return false;
		}
		// 20日均线在30日之上
		int whiteHorseTmp = 0;
		for (int i = 0; i < clist10.size(); i++) {
			StockAvgBase sa = clist10.get(i);
			if (isTrace && sa.getDate() == date) {
				continue;
			}
			if (clist10.get(i).getAvgPriceIndex20() >= clist10.get(i).getAvgPriceIndex30()) {
				whiteHorseTmp++;
			}
		}
		if (whiteHorseTmp >= 5) {
			return true;
		}

		// 各均线在2.5%之内波动
		if (whiteHorseTmp >= 3) {
			for (int i = 0; i < clist10.size(); i++) {
				StockAvgBase sa = clist10.get(i);
//				if (isTrace && sa.getDate() == date) {
//					continue;
//				}
				List<Double> l = Arrays.asList(sa.getAvgPriceIndex5(), sa.getAvgPriceIndex10(), sa.getAvgPriceIndex20(),
						sa.getAvgPriceIndex30());
				double max2 = l.stream().max((p1, p2) -> p1.compareTo(p2)).get();
				double min2 = l.stream().min((p1, p2) -> p1.compareTo(p2)).get();

				if (CurrencyUitl.cutProfit(min2, max2) > 2.5) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		List<Double> l = Arrays.asList(1.1d, 2d, 3d, 4.0d, 4.1d);
		System.err.println(l.stream().max((p1, p2) -> p1.compareTo(p2)).get());
		System.err.println(l.stream().min((p1, p2) -> p1.compareTo(p2)).get());
	}

	public boolean isStable(String code, int buyDate) {
		List<StockAvgBase> clist10 = avgService.queryListByCodeForModelWithLast(code, buyDate,
				EsQueryPageUtil.queryPage10, true);
		StockAvgBase buy = clist10.get(0);// 买入日期
		StockAvgBase sa1 = clist10.get(1);// 前日
		if (buy.getAvgPriceIndex5() >= sa1.getAvgPriceIndex5()// 5日线企稳
				&& buy.getAvgPriceIndex5() > buy.getAvgPriceIndex10()) {// 5日线>10日线
			return true;
		}
		return false;
	}
}
