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
@Document(indexName = "dzjy_yi_time")
public class DzjyYiTime extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Field(type = FieldType.Keyword)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Double)
	private double totalAmt;// 总额-1年
	@Field(type = FieldType.Double)
	private double avgPrcie;// 均价-1年
	@Field(type = FieldType.Double)
	private double totalAmt60d;// 总额-60天
	@Field(type = FieldType.Double)
	private double p60d;// 流通占比（-60天）
	@Field(type = FieldType.Double)
	private double p365d;// 流通占比（-1年）
}
