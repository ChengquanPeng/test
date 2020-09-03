package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StockAType {
	SHM(1, "60", "上海主板"), SZM(2, "00", "深圳主板"), CYB(4, "30", "深圳创业板"), KCB(5, "68", "上海科创板");

	private int code;
	private String startWith;
	private String desc;

	public static StockAType formatCode(String code) {
		if (code.startsWith(StockAType.SHM.getStartWith())) {
			return StockAType.SHM;
		} else if (code.startsWith(StockAType.SZM.getStartWith())) {
			return StockAType.SZM;
		} else if (code.startsWith(StockAType.CYB.getStartWith())) {
			return StockAType.CYB;
		} else if (code.startsWith(StockAType.KCB.getStartWith())) {
			return StockAType.KCB;
		}
		return null;
	}

	public static boolean isTop20(String code) {
		StockAType sa = StockAType.formatCode(code);
		if (StockAType.KCB == sa || StockAType.CYB == sa) {
//		if (StockAType.KCB == sa) {
			return true;
		}
		return false;
	}
}
