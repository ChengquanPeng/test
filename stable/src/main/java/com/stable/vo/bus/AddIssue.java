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
@Document(indexName = "add_issue")
public class AddIssue extends EsBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int startDate;
	@Field(type = FieldType.Integer)
	private int endDate;
	@Field(type = FieldType.Integer)
	private int status;// 1开始，2完成，3终止
	@Field(type = FieldType.Text)
	private String titles;

	public AddIssue() {

	}

}
