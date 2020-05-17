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
@Document(indexName = "concept_daily")
public class ConceptDaily extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8129067850969692849L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String conceptId;
	@Field(type = FieldType.Integer)
	private int date;

	@Field(type = FieldType.Double)
	private double open;
	@Field(type = FieldType.Double)
	private double high;
	@Field(type = FieldType.Double)
	private double low;
	@Field(type = FieldType.Double)
	private double close;
	@Field(type = FieldType.Long)
	private long vol;
	@Field(type = FieldType.Double)
	private long amt;
	@Field(type = FieldType.Double)
	private double todayChange;

	public void setId() {
		id = this.conceptId + date;
	}
}
