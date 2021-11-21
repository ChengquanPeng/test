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
@Document(indexName = "holder_num")
public class HolderNum extends EsBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3583785305111756892L;
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
	private int num;// 人数
	@Field(type = FieldType.Double)
	private double avgPrice;// 股价
	@Field(type = FieldType.Integer)
	private int sysdate;
	@Field(type = FieldType.Long)
	private long avgNumP5;// 除开5%以上股东外的人均持股

}
