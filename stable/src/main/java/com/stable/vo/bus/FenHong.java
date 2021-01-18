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
@Document(indexName = "fen_hong")
public class FenHong extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int times;// 次数
	@Field(type = FieldType.Double)
	private double price;// 金额
	@Field(type = FieldType.Integer)
	private int update;
	@Field(type = FieldType.Text)
	private String details;//详细
	public FenHong() {

	}

}
