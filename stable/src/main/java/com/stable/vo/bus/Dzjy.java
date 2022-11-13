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
public class Dzjy extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Double)
	private double price;// 价格
	@Field(type = FieldType.Double)
	private double tvol;// 交易量（万股)
	@Field(type = FieldType.Double)
	private double tval;// 交易额（万元）
	@Field(type = FieldType.Text)
	private String buyername;// 买方
	@Field(type = FieldType.Text)
	private String salesname;// 卖方
	@Field(type = FieldType.Double)
	private double rchange;// 溢折价
	@Field(type = FieldType.Text)
	private String saleCode;// saleCode
	@Field(type = FieldType.Text)
	private String buyCode;// buyCode
	@Field(type = FieldType.Integer)
	private int dailyRank;// dailyRank

	public void setId() {
		id = code + date + "|" + tval + "|" + saleCode + "|" + buyCode;
	}
}
