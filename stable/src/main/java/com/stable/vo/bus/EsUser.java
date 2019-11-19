package com.stable.vo.bus;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "user")
public class EsUser extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3102074407008461165L;

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String username;
	@Field(type = FieldType.Text)
	private String age;
	@Field(type = FieldType.Long)
	private Date ctm;
}
