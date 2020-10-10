package com.stable.service.model.data;

import java.util.List;

import com.stable.vo.ModelContext;
import com.stable.vo.bus.StockAvg;

//@Log4j2
public class LineAvgPrice {
	// construction
	private AvgService avgService;
	private String code;
	private int date;

	// TEMP
	public StockAvg todayAv;

	public LineAvgPrice(AvgService avgService, ModelContext cxt) {
		this.avgService = avgService;
		this.code = cxt.getCode();
		this.date = cxt.getDate();
	}

	public LineAvgPrice(String code, int date, AvgService avgService) {
		this.code = code;
		this.date = date;
		this.avgService = avgService;
	}

	private boolean isWhiteHorseV2Get = false;
	private boolean isWhiteHorseV2Res = false;

	// 类似白马(30个交易日，20日均线一直在30日均线之上（更加宽松比V1）
	public boolean isWhiteHorseV2() {
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

}
