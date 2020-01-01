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
	private int the6thDay;
	@Field(type = FieldType.Integer)
	private int isThe6thDayUp;
	@Field(type = FieldType.Double)
	private double the6thChange;
	@Field(type = FieldType.Double)
	private double the6thChangeRate;
}
