package com.stable.vo;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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

	@Field(type = FieldType.Double)
	private double goodWill; // 商誉
	@Field(type = FieldType.Double)
	private double sumLasset; // 流动资产合计
	@Field(type = FieldType.Double)
	private double sumAsset; // 总资产
	@Field(type = FieldType.Double)
	private double inventory; // 存货资产
	@Field(type = FieldType.Double)
	private double sumDebt; // 负债
	@Field(type = FieldType.Double)
	private double sumDebtLd; // 流动负债
	@Field(type = FieldType.Double)
	private double netAsset; // 净资产
	@Field(type = FieldType.Double)
	private double monetaryFund;// 货币资金
	@Field(type = FieldType.Double)
	private double tradeFinassetNotfvtpl; // 交易性金融资产
	@Field(type = FieldType.Double)
	private double accountrec;// 应收账款（是否自己贴钱在干活，同行业比较）
	@Field(type = FieldType.Double)
	private double accountPay;// 应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
	@Field(type = FieldType.Double)
	private double retaineDearning;// 未分配利润
	@Field(type = FieldType.Double)
	private double interestPay;// 应付利息:如果较高，公司在大量有息借钱，关联到货币资金和未分配利润。如果货币资金和未分配利润较高，明明有钱为什么借钱，

}
