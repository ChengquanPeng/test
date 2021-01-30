package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "foreign_capital_sum")
public class ForeignCapitalSum extends EsBase {
	private static final long serialVersionUID = 1L;
	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int date; //
	@Field(type = FieldType.Long)
	private long holdVol; // 数量
	@Field(type = FieldType.Double)
	private double holdAmount; // 金额
	@Field(type = FieldType.Double)
	private double holdRatio; // 比例
}
