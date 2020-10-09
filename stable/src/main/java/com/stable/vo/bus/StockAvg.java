package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.ToString;

@ToString
@Document(indexName = "stock_avg")
public class StockAvg extends EsBase {
	private static final long serialVersionUID = 107721876820773361L;
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

	public int getLastDividendDate() {
		return lastDividendDate;
	}

	public void setLastDividendDate(int lastDividendDate) {
		this.lastDividendDate = lastDividendDate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getDate() {
		return date;
	}

	public void setDate(int date) {
		this.date = date;
	}

	public double getAvgPriceIndex5() {
		return avgPriceIndex5;
	}

	public void setAvgPriceIndex5(double avgPriceIndex5) {
		this.avgPriceIndex5 = avgPriceIndex5;
	}

	public double getAvgPriceIndex10() {
		return avgPriceIndex10;
	}

	public void setAvgPriceIndex10(double avgPriceIndex10) {
		this.avgPriceIndex10 = avgPriceIndex10;
	}

	public double getAvgPriceIndex20() {
		return avgPriceIndex20;
	}

	public void setAvgPriceIndex20(double avgPriceIndex20) {
		this.avgPriceIndex20 = avgPriceIndex20;
	}

	public double getAvgPriceIndex30() {
		return avgPriceIndex30;
	}

	public void setAvgPriceIndex30(double avgPriceIndex30) {
		this.avgPriceIndex30 = avgPriceIndex30;
	}

	public double getAvgPriceIndex60() {
		return avgPriceIndex60;
	}

	public void setAvgPriceIndex60(double avgPriceIndex60) {
		this.avgPriceIndex60 = avgPriceIndex60;
	}

	public double getAvgPriceIndex120() {
		return avgPriceIndex120;
	}

	public void setAvgPriceIndex120(double avgPriceIndex120) {
		this.avgPriceIndex120 = avgPriceIndex120;
	}

	public double getAvgPriceIndex250() {
		return avgPriceIndex250;
	}

	public void setAvgPriceIndex250(double avgPriceIndex250) {
		this.avgPriceIndex250 = avgPriceIndex250;
	}
}
