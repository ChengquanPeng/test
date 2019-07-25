package com.stable.es.vo;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

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
	@Field
	private String username;
	@Field
	private String age;
	@Field
	private Date ctm;
}
