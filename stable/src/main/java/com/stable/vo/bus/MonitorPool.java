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
@Document(indexName = "monitor_pool")
public class MonitorPool {
	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int updateDate = 0;
	// ---------------------------
	// 监听:0不监听，1大牛，2中线，3人工，4短线，5.增发监听
	@Field(type = FieldType.Integer)
	private int monitor = 0;
	@Field(type = FieldType.Text)
	private String remark;

	@Field(type = FieldType.Integer)
	private int realtime = 0;// 实时
	@Field(type = FieldType.Integer)
	private int offline = 0;// 收盘
	@Field(type = FieldType.Double)
	private double upPrice = 0;// 高于某价格
	@Field(type = FieldType.Integer)
	private double downPrice = 0;// 低于某价格
	@Field(type = FieldType.Double)
	private double upTodayChange = 0;// 高于某涨幅
	@Field(type = FieldType.Integer)
	private double downTodayChange = 0;// 低于某涨幅
}
