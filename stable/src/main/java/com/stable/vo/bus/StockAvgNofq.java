package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Document(indexName = "stock_avg_nofq")
public class StockAvgNofq extends EsBase implements StockAvgBase {
	private static final long serialVersionUID = 107721876820773362L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int date;

	@Field(type = FieldType.Double)
	private double avgPriceIndex5;
	@Field(type = FieldType.Double)
	private double avgPriceIndex10;
	@Field(type = FieldType.Double)
	private double avgPriceIndex20;
	@Field(type = FieldType.Double)
	private double avgPriceIndex30;
	@Field(type = FieldType.Double)
	private double avgPriceIndex60;
	@Field(type = FieldType.Double)
	private double avgPriceIndex120;
	@Field(type = FieldType.Double)
	private double avgPriceIndex250;
	@Field(type = FieldType.Integer)
	private int lastDividendDate;

	public void setId() {
		id = code + date;
	}
}
