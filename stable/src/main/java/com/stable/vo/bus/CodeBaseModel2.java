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
}
