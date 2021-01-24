package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "code_base_model2")
public class CodeBaseModel2 extends EsBase {
	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	protected int date;
	@Field(type = FieldType.Integer)
	protected int currYear;
	@Field(type = FieldType.Integer)
	protected int currQuarter;

	@Field(type = FieldType.Integer)
	protected int baseRed;// 基本面：红色
	@Field(type = FieldType.Text)
	private String baseRedDesc;
	@Field(type = FieldType.Integer)
	protected int baseYellow;// 基本面：黄色
	@Field(type = FieldType.Text)
	private String baseYellowDesc;
	@Field(type = FieldType.Integer)
	protected int baseBlue;// 基本面：蓝色
	@Field(type = FieldType.Text)
	private String baseBlueDesc;
	@Field(type = FieldType.Integer)
	protected int baseGreen;// 基本面：绿色
	@Field(type = FieldType.Text)
	private String baseGreenDesc;

	// 加权净资产收益率
	@Field(type = FieldType.Double)
	private double syl;// 加权净资产收益率
	@Field(type = FieldType.Double)
	private double syldjd;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Double)
	private double sylttm;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Integer)
	private int sylType;// 收益率类型:1:自身收益率增长,2: 年收益率超过5.0%*4=20%,4:同时包含12

	// 增发
	@Field(type = FieldType.Integer)
	private int zfStatus;// 增发状态（近1年）: 0无增发，1增发中，2增发完成，3，增发终止
	@Field(type = FieldType.Text)
	private String zfStatusDesc;// 增发进度
	
	
	//
}
