package com.stable.vo.up.strategy;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "strategy_shs_fivedaysdown")
public class ShFiveDaysDown {

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int firstDay;
	@Field(type = FieldType.Integer)

	// 3日表现
	private int the6thDay;
	@Field(type = FieldType.Integer)
	private int isThe6thDayUp;
	@Field(type = FieldType.Double)
	private double the6thChange;
	@Field(type = FieldType.Double)
	private double the6thChangeRate;
	@Field(type = FieldType.Integer)
	private int isThe7thDayUp;
	@Field(type = FieldType.Double)
	private double the7thChange;
	@Field(type = FieldType.Double)
	private double the7thChangeRate;
	@Field(type = FieldType.Integer)
	private int isThe8thDayUp;
	@Field(type = FieldType.Double)
	private double the8thChange;
	@Field(type = FieldType.Double)
	private double the8thChangeRate;

	public void genRecordId() {
		this.id = code + firstDay;
	}
}
