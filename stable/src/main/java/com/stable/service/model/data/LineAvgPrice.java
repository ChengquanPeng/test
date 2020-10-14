package com.stable.service.model.data;

import java.util.List;

import com.stable.constant.EsQueryPageUtil;
import com.stable.vo.bus.StockAvg;

//@Log4j2
public class LineAvgPrice {
	// construction
	private AvgService avgService;

	// TEMP
	public StockAvg todayAv;

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
		List<StockAvg> clist30 = avgService.queryListByCodeForModelWithLastQfqAnd30Records(code, date);
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

	public boolean isWhiteHorseV3ForMiddle(String code, int date) {
		// 最近10条
		List<StockAvg> clist10 = avgService.queryListByCodeForModelWithLastQfq(code, date, EsQueryPageUtil.queryPage10);
		// todayAv = clist10.get(0);
		int whiteHorseTmp = 0;
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

	public boolean isStable(String code, int date) {
		List<StockAvg> clist10 = avgService.queryListByCodeForModelWithLastQfq(code, date, EsQueryPageUtil.queryPage10);
		StockAvg sa0 = clist10.get(0);
		StockAvg sa1 = clist10.get(1);
		if (sa1.getAvgPriceIndex5() >= sa0.getAvgPriceIndex5()// 5日线企稳
				&& sa1.getAvgPriceIndex5() > sa1.getAvgPriceIndex10()) {// 5日线>10日线
			return true;
		}
		return false;
	}
}
