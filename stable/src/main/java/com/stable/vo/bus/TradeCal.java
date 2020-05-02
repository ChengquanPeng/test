package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "trade_cal")
public class TradeCal extends EsBase {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1332874838120670704L;
	@Id
	@Field(type = FieldType.Integer)
	private int cal_date;
	@Field(type = FieldType.Integer)
	private int is_open = 1;
	@Field(type = FieldType.Integer)
	private int pretrade_date;
}
