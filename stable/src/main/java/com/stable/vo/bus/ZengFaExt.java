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
	private int date;// 进度
	@Field(type = FieldType.Text)
	private String title;// 发行类型
}
