package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "tick_data_buy_sell")
public class TickDataBuySellInfo extends EsBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6579495705405954727L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int date;
	@Field(type = FieldType.Long)
	private Long buyTotalAmt;
	@Field(type = FieldType.Long)
	private Long sellTotalAmt;
	@Field(type = FieldType.Long)
	private Long otherTotalAmt;
	@Field(type = FieldType.Long)
	private Long totalAmt;

	@Field(type = FieldType.Long)
	private Long buyTotalVol;
	@Field(type = FieldType.Long)
	private Long sellTotalVol;
	@Field(type = FieldType.Long)
	private Long otherTotalVol;
	@Field(type = FieldType.Long)
	private Long totalVol;
	@Field(type = FieldType.Integer)
	private int programRate;
	
	@Field(type = FieldType.Integer)
	private Long sellTimes;
	@Field(type = FieldType.Integer)
	private Long buyTimes;
	@Field(type = FieldType.Integer)
	private Long otherTimes;

	public TickDataBuySellInfo() {
	}

	public void setKey() {
		id = code + date;
	}
}
