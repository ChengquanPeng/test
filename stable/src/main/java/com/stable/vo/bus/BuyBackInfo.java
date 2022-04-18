package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "buy_back")
public class BuyBackInfo extends EsBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3583785395111756892L;
	/**
	 * 
	 */
	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private String code;

	@Field(type = FieldType.Integer)
	private int date;

	@Field(type = FieldType.Integer)
	private int status;

	@Field(type = FieldType.Integer)
	private int type;// 1.增持，2.回购

	@Field(type = FieldType.Text)
	private String desc;

	public BuyBackInfo() {

	}
}
