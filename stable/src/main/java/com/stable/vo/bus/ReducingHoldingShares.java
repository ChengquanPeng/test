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
@Document(indexName = "reducing_holding_shares")
public class ReducingHoldingShares extends EsBase {
	private static final long serialVersionUID = 1L;

	@Id
	private String id;// code+date

	@Field(type = FieldType.Keyword)
	private String code;// code

	@Field(type = FieldType.Integer)
	private int date = 0;// 日期date

	@Field(type = FieldType.Integer)
	private int type;// 1.股东减持,2.增减持计划,3.高管减持,4.其他

	@Field(type = FieldType.Double)
	private double wg;// 万股

	@Field(type = FieldType.Text)
	private String desc;

}
