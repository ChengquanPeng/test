package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StockAType {
	SHM(1, "60", "上海主板"), SZM(2, "0", "深圳主板"), CYB(4, "3", "深圳创业板"), KCB(5, "68", "上海科创板");

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

	// 2020年8月24日创业板注册制
	private static int CYB_TOP20_DATE = 20200824;

	public static boolean isTop20(String code, int date) {
		StockAType sa = StockAType.formatCode(code);
		if (StockAType.KCB == sa) {
			return true;
		} else if (StockAType.CYB == sa) {
			if (date >= CYB_TOP20_DATE) {
				return true;
			}
		}
		return false;
	}
}
