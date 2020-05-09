package com.stable.vo.up.strategy;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "modelv1")
public class ModelV1 {

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int date;
	@Field(type = FieldType.Double)
	private double close;
	@Field(type = FieldType.Integer)
	private int score = 0;
	// L1
	// 1强势:短中长期强势=>次数和差值:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int sortStrong = 0;
//	@Field(type = FieldType.Integer)
//	private int midStrong = 0;
//	@Field(type = FieldType.Integer)
//	private int lngStrong = 0;
	// 2交易方向:次数和差值:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int sortWay = 0;
//	@Field(type = FieldType.Integer)
//	private int midWay = 0;
//	@Field(type = FieldType.Integer)
//	private int lngWay = 0;
	// 3程序单:次数:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int sortPgm = 0;
//	@Field(type = FieldType.Integer)
//	private int midPgm = 0;
//	@Field(type = FieldType.Integer)
//	private int lngPgm = 0;
	// 4均线:指数:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int avgIndex = 0;
	@Field(type = FieldType.Integer)
	private int volIndex = 0;
	// 3天的价格振幅
	@Field(type = FieldType.Integer)
	private int sortPriceIndex = 0;
	// 价格底部指数
	@Field(type = FieldType.Integer)
	private int priceIndex = 0;
	// 图形
	@Field(type = FieldType.Integer)
	private int imageIndex = 0;
	// L2
	// 板块活跃度
	// 回购/分红/业绩
}
