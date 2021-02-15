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
	private double jqjzcsyl; // 加权净资产收益率(%) -加权净资产收益率=当期净利润/当期加权平均净资产
	private String jyxjl; // 每股经营现金流
	private String accountrec; // 应收账款
	
	private double mgjyxjl; // 每股经营现金流
	private double mll; // 毛利率
	
	private double zcfzl; // 资产负债率
	private String sumLasset; // 流动资产合计
	private String sumDebtLd; // 流动负债总计
	private String netAsset; // 净资产

	public void setEndType(int year, int q) {
		if (q == 4) {
			endType = year + "-年报";
		} else {
			endType = year + "-" + q;
		}
	}
}
