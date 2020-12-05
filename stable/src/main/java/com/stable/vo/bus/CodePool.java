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
@Document(indexName = "code_pool")
public class CodePool {
	@Id
	private String code;
	
	@Field(type = FieldType.Integer)
	private String code2;

	@Field(type = FieldType.Integer)
	private int mid = 0;

	@Field(type = FieldType.Integer)
	private int sort = 0;

	@Field(type = FieldType.Text)
	private String remark;
}
