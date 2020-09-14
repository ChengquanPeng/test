package com.stable.vo.retrace;

import lombok.Data;

@Data
public class TraceSortv2Vo {
	private String code;
	private int date;
	private boolean isOk = false;
	private double profit;// 盈利

	public boolean isOk() {
		return isOk;
	}

	public void setOk(boolean isOk) {
		this.isOk = isOk;
	}

	public String toDetailStr() {
		return "TraceSortv1Vo:" + code + "," + date + "," + (isOk ? "Y" : "N");
	}

	public String toDetailStrShow() {
		return code + "@" + date;
	}
}
