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
@Document(indexName = "fin_yjyg")
public class FinYjyg extends EsBase {
	private static final long serialVersionUID = -6862288561607105101L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int date; // 报告日期
	@Field(type = FieldType.Integer)
	private int year;// 年
	@Field(type = FieldType.Integer)
	private int quarter;// 季度
	@Field(type = FieldType.Keyword) // 预增/预减/扭亏/首亏/续亏/续盈/略增/略减
	private String type;// 类型:预增/预减/扭亏/首亏/续亏/续盈/略增/略减
	@Field(type = FieldType.Long)
	private long jlr;// 预计利润
	@Field(type = FieldType.Double)
	private double jlrtbzz; // 业绩变动幅度(%)
	@Field(type = FieldType.Integer)
	private int updateDate;// 更新日期

	public void setId() {
		id = code + date;
	}
}
