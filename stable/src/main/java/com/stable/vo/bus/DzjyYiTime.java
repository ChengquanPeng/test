package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.ToString;

@ToString
@Document(indexName = "dzjy_yi_time")
public class DzjyYiTime {
	@Id
	@Field(type = FieldType.Text)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Double)
	private double totalAmt;// 总额
	@Field(type = FieldType.Double)
	private double avgPrcie;// 均价
	@Field(type = FieldType.Double)
	private double totalAmt60d;// 总额

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

	public double getTotalAmt() {
		return totalAmt;
	}

	public void setTotalAmt(double totalAmt) {
		this.totalAmt = totalAmt;
	}

	public double getAvgPrcie() {
		return avgPrcie;
	}

	public void setAvgPrcie(double avgPrcie) {
		this.avgPrcie = avgPrcie;
	}

	public double getTotalAmt60d() {
		return totalAmt60d;
	}

	public void setTotalAmt60d(double totalAmt60d) {
		this.totalAmt60d = totalAmt60d;
	}

}
