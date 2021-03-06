package com.stable.service.model.data;

import com.stable.vo.bus.FinanceBaseInfo;

public class FinanceAnalyzer {

	private FinanceBaseInfo currYear;// 本年
	private FinanceBaseInfo prevYear;// 上年

	private FinanceBaseInfo currJidu;// 当前季度
	private FinanceBaseInfo prevJidu;// 上前季度

	public String printInfo() {
		StringBuffer sb = new StringBuffer();
		if (currJidu != null) {
			sb.append("当前季度:").append(currJidu.toString());
		}
		if (prevJidu != null) {
			sb.append("上一季度:").append(prevJidu.toString());
		}
		if (currYear != null) {
			sb.append("最新年度:").append(currYear.toString());
		}
		if (prevYear != null) {
			sb.append("上级年度:").append(prevYear.toString());
		}
		return sb.toString();
	}

	public void putJidu1(FinanceBaseInfo fbi) {
		if (currJidu == null) {
			this.currJidu = fbi;
		} else if (prevJidu == null) {
			this.prevJidu = fbi;
		}
		if (fbi.getQuarter() == 4) {
			if (currYear == null) {
				currYear = fbi;
			} else if (prevYear == null) {
				prevYear = fbi;
			}
		}
	}

	// 最近2个季度
	// 营收
	public int incomeUp2Quarter() {
		if (currJidu.getYyzsrtbzz() > 0 && prevJidu.getYyzsrtbzz() > 0) {
			return 1;
		}
		return 0;
	}

	public int incomeDown2Quarter() {
		if (currJidu.getYyzsrtbzz() < 0 && prevJidu.getYyzsrtbzz() < 0) {
			return 1;
		}
		return 0;
	}

	// 利润
	public int profitUp2Quarter() {
		if (currJidu.getGsjlrtbzz() > 0 && prevJidu.getGsjlrtbzz() > 0) {
			return 1;
		}
		return 0;
	}

	public int profitDown2Quarter() {
		if (currJidu.getGsjlrtbzz() < 0 && prevJidu.getGsjlrtbzz() < 0) {
			return 1;
		}
		return 0;
	}

	// 退市
	public int profitDown2Year() {
		if (currYear.getGsjlr() < 0 && prevYear != null && prevYear.getGsjlr() < 0) {
			return 1;
		}
		return 0;
	}

	public FinanceBaseInfo getCurrYear() {
		return currYear;
	}

	public FinanceBaseInfo getPrevYear() {
		return prevYear;
	}

	public FinanceBaseInfo getCurrJidu() {
		return currJidu;
	}

	public FinanceBaseInfo getPrevJidu() {
		return prevJidu;
	}

}
