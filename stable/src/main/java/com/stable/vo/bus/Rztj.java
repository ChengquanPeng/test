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
@Document(indexName = "rztj")
public class Rztj extends EsBase {
	private static final long serialVersionUID = 1L;
	@Id
	@Field(type = FieldType.Keyword)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int updateDate = 0;
	@Field(type = FieldType.Double)
	private double totalAmt;// 余额
	@Field(type = FieldType.Double)
	private double avgAmt;// 20天均额
	@Field(type = FieldType.Integer)
	private int validDate = 0;// 有效期
	@Field(type = FieldType.Double)
	private int valid = 0;// 是否有效
}
