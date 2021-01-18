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
@Document(indexName = "zeng_fa_detail")
public class ZengFaDetail extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int date;// 日期
	@Field(type = FieldType.Text)
	private String details;//详细
	@Field(type = FieldType.Integer)
	private int update;
	public ZengFaDetail() {

	}

}
