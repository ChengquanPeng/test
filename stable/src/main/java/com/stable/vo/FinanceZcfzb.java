package com.stable.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class FinanceZcfzb {
	private String id;
	private String code;
	private int date; // 报告日期

	private double goodWill; // 商誉
	private double sumLasset; // 流动资产合计
	private double sumAsset; // 总资产
	private double inventory; // 存货资产
	private double sumDebt; // 负债
	private double sumDebtLd; // 流动负债
	private double netAsset; // 净资产
	private double accountrec;// 应收账款（是否自己贴钱在干活，同行业比较）
	private double accountPay;// 应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
	private double retaineDearning;// 未分配利润
	private double interestPay;// 应付利息:如果较高，公司在大量有息借钱，关联到货币资金和未分配利润。如果货币资金和未分配利润较高，明明有钱为什么借钱，

	private double monetaryFund;// 货币资金
	private double tradeFinassetNotfvtpl; // 交易性金融资产
	private double stborrow;// 短期借款
	private double ltborrow;// 长期借款
	private double intangibleAsset; // 无形资产
}
