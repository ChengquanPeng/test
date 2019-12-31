package com.stable.vo.http.resp;

import lombok.Data;

@Data
public class FinanceBaseInfoResp {

	private String code;
	private String codeName;
	private String endDate;
	private String annDate;
	private String totalRevenue;
	private String totalCogs;
	private String operateProfit;
	private String totalProfit;
	private String incomeTax;
	private String income;
	private String finExp;
	private String basicEps;

	public void setEndDate(int year, int q) {
		if (q == 4) {
			endDate = year + "-年报";
		} else {
			endDate = year + "-" + q;
		}
	}
}
