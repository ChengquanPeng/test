package com.stable.vo;

import lombok.Data;

@Data
public class QiBaoInfo {

	private int date;
	private double price;
	private double vol;
	private double yesterdayPrice;
	private double low;
	private double chkRate = 15;
	private int syx = 0;
	private int dyx = 0;

	public String ex() {
		String ex = "";
		if (syx == 1) {
			ex = "上影线";
		}
		if (dyx == 1) {
			ex = ex.length() > 0 ? ex + ",大阴线" : "大阴线";
		}
		return ex;
	}
}
