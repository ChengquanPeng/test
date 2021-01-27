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
@Document(indexName = "zeng_fa_ext")
public class ZengFaExt extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int buy;// 0无，1购买资产 
	@Field(type = FieldType.Integer)
	private int selfzf;//自己人在增发， 0无，1确定，2不确定 
	@Field(type = FieldType.Integer)
	private int compType;//企业类型， 0未知，1国企，2民企 
	@Field(type = FieldType.Double)
	private double totalMarketVal;//总市值
	@Field(type = FieldType.Double)
	private double circMarketVal;//流通市值
	@Field(type = FieldType.Integer)
	private int date;// 公告时间
	@Field(type = FieldType.Text)
	private String title;// 发行类型
}
