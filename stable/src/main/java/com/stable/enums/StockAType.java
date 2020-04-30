package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StockAType {
	SHM(1, "60", "上海主板"), SZM(2, "000", "深圳主板"), ZXB(3, "002", "深圳中小板"), CYB(4, "300", "深圳创业板"), KCB(5, "68", "上海科创板");

	private int code;
	private String startWith;
	private String desc;
	
	public static StockAType formatCode(String code) {
		if (code.startsWith(StockAType.SHM.getStartWith())) {
			return StockAType.SHM;
		} else if (code.startsWith(StockAType.SZM.getStartWith())) {
			return StockAType.SZM;
		} else if (code.startsWith(StockAType.ZXB.getStartWith())) {
			return StockAType.ZXB;
		} else if (code.startsWith(StockAType.CYB.getStartWith())) {
			return StockAType.CYB;
		} else if (code.startsWith(StockAType.KCB.getStartWith())) {
			return StockAType.KCB;
		}
		return null;
	}
}
