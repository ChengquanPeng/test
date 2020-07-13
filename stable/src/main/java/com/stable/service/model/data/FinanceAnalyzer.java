package com.stable.service.model.data;

import com.stable.vo.bus.FinanceBaseInfo;

public class FinanceAnalyzer {

	private FinanceBaseInfo yearCurr;
	private FinanceBaseInfo previousCurr;

	public void putYear(FinanceBaseInfo year) {
		if (yearCurr == null) {
			yearCurr = year;
		} else if (previousCurr == null) {
			previousCurr = year;
		}
	}

	public int getincomeUp2yearc() {
		if (yearCurr != null && previousCurr != null) {
			return yearCurr.getYyzsr() > previousCurr.getYyzsr() ? 1 : 0;
		}
		return 0;
	}
}
