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
@Document(indexName = "dzjy")
public class Dzjy {
	@Id
	private String id;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Text)
	private String SECUCODE;
	@Field(type = FieldType.Double)
	private double PRICE;// 价格
	@Field(type = FieldType.Double)
	private double TVOL;// 交易量
	@Field(type = FieldType.Double)
	private double TVAL;// 交易额
	@Field(type = FieldType.Text)
	private String BUYERNAME;// 买方
	@Field(type = FieldType.Text)
	private String SALESNAME;// 卖方
	@Field(type = FieldType.Double)
	private double RCHANGE;// 溢折价

	public void setId() {
		id = SECUCODE + date;
	}
}
