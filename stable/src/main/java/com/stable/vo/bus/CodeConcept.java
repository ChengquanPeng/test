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
@Document(indexName = "code_concept")
public class CodeConcept extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8129067850969692849L;
	@Id
	private String id;
	@Field(type = FieldType.Text, fielddata = true)
	private String code;
	@Field(type = FieldType.Text)
	private String conceptId;
	@Field(type = FieldType.Text)
	private String conceptName;
	@Field(type = FieldType.Integer)
	private int type;
	@Field(type = FieldType.Integer)
	private int updateTime;
}
