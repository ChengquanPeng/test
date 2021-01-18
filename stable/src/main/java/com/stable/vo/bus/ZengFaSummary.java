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
@Document(indexName = "zeng_fa_summary")
public class ZengFaSummary extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String code;
	@Field(type = FieldType.Text)
	private String desc;
	@Field(type = FieldType.Integer)
	private int update;

	public ZengFaSummary() {

	}

}
