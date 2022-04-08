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
@Document(indexName = "prd1")
public class Prd1 {

	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int prd;// 是否产品
	@Field(type = FieldType.Integer)
	private int prdsub;// 产品分类
	@Field(type = FieldType.Integer)
	private int buying;// 状态：是否已买入
}
