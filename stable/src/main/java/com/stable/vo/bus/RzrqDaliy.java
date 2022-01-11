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
@Document(indexName = "rzrq_daliy")
public class RzrqDaliy {
	@Id
	private String id;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Double)
	private double balance;// 融资余额
	@Field(type = FieldType.Double)
	private double mr;// 买入额
	@Field(type = FieldType.Double)
	private double mc;// 买入额
	@Field(type = FieldType.Double)
	private double jmr;// 净买入

	public void setId() {
		id = code + date;
	}
}