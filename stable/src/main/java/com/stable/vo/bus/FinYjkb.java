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
@Document(indexName = "fin_yjkb")
public class FinYjkb extends EsBase {
	private static final long serialVersionUID = 2305594280230790359L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int date; // 报告日期
	@Field(type = FieldType.Integer)
	private int year;// 年
	@Field(type = FieldType.Integer)
	private int quarter;// 季度

	@Field(type = FieldType.Long)
	private long yyzsr; // 营业总收入
	@Field(type = FieldType.Long)
	private long jlr; // 归属净利润

	@Field(type = FieldType.Double)
	private double yyzsrtbzz; // 营业总收入同比增长(%)
	@Field(type = FieldType.Double)
	private double jlrtbzz; // 归属净利润同比增长(%)
	@Field(type = FieldType.Integer)
	private int updateDate;// 更新日期

	@Field(type = FieldType.Integer)
	private int annDate;

	public void setId() {
		id = code + date;
	}
}
