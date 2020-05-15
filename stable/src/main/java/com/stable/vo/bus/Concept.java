package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "concept")
public class Concept extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8129067850969692849L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String name;
	@Field(type = FieldType.Integer)
	private String date;
}
