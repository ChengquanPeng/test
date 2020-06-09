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

	// 模板类型
	@Field(type = FieldType.Integer)
	private int modelType = 0;
	// 分数
	@Field(type = FieldType.Integer)
	private int score = 0;

	// L1
	// 1强势:短中长期强势=>次数和差值:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int sortStrong = 0;
	// 2交易方向:次数和差值:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int sortWay = 0;
	@Field(type = FieldType.Integer)
	private int sortPgm = 0;
	// 4均线:指数:3/5/10/20/120/250天
	@Field(type = FieldType.Integer)
	private int avgScore = 0;
	// 价格底部指数
	@Field(type = FieldType.Integer)
	private int priceIndex = 0;
	// 是否白马股
	@Field(type = FieldType.Integer)
	private int whiteHorse = 0;
	// L2
	// 板块活跃度
	@Field(type = FieldType.Integer)
	private int gnScore;
	// 周线是否OK
	@Field(type = FieldType.Integer)
	private int weekOk = 0;
	// 超过30%的波动
	@Field(type = FieldType.Integer)
	private int isRange30p = 0;

	// 图形结果
	@Field(type = FieldType.Text)
	private String imgResult;
	// 回购/分红/业绩
}
