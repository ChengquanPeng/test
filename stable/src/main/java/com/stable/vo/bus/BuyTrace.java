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
@Document(indexName = "buy_trace")
public class BuyTrace extends EsBase {
	private static final long serialVersionUID = -6862288561607105101L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int buyDate;
	@Field(type = FieldType.Double)
	private double buyPrice;
	@Field(type = FieldType.Integer)
	private int status;// 1:waiting buy(等待成交),2 bought(已买),3 sold(已卖)
	@Field(type = FieldType.Integer)
	private int soldDate;
	@Field(type = FieldType.Double)
	private double soldPrice;
	@Field(type = FieldType.Double)
	private double profit;
	@Field(type = FieldType.Integer)
	private int buyModelType;// 人工还是机器
	@Field(type = FieldType.Integer)
	private int ver;// 版本
	@Field(type = FieldType.Keyword)
	private String subVer;// 版本

	public void setId() {
		id = code + buyDate + ver + subVer;
	}
}
