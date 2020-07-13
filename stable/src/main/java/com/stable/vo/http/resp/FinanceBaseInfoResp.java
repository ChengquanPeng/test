package com.stable.vo.http.resp;

import lombok.Data;

@Data
public class FinanceBaseInfoResp {

	private String code;
	private String codeName;
	private String endType;
	private String endDate;
	private String yyzsr; // 营业总收入
	private String gsjlr; // 归属净利润
	private String kfjlr; // 扣非净利润
	private double yyzsrtbzz; // 营业总收入同比增长(%)
	private double gsjlrtbzz; // 归属净利润同比增长(%)
	private double kfjlrtbzz; // 扣非净利润同比增长(%)

	public void setEndType(int year, int q) {
		if (q == 4) {
			endType = year + "-年报";
		} else {
			endType = year + "-" + q;
		}
	}
}
