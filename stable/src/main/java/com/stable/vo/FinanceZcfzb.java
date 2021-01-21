package com.stable.vo;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class FinanceZcfzb{
	private String id;
	private String code;
	private int date; // 报告日期
	
	@Field(type = FieldType.Double)
	private double goodWill; // 商誉
	@Field(type = FieldType.Double)
	private double sumAsset; // 总资产
	@Field(type = FieldType.Double)
	private double inventory; // 存货资产
	@Field(type = FieldType.Double)
	private double sumDebt; // 负债
	@Field(type = FieldType.Double)
	private double netAsset; // 净资产
}
