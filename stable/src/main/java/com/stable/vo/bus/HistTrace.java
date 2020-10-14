package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "hist_trace")
public class HistTrace extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	@Field(type = FieldType.Text)
	private String id;
	@Field(type = FieldType.Integer)
	private int batch;

	// 条件
	@Field(type = FieldType.Integer)
	private int startDate;
	@Field(type = FieldType.Integer)
	private int endDate;
	@Field(type = FieldType.Text)
	private String version;
	@Field(type = FieldType.Integer)
	private int days;
	@Field(type = FieldType.Double)
	private double vol;
	@Field(type = FieldType.Integer)
	private int oneYear = 1;// 1年未大涨

	// 结果

	@Field(type = FieldType.Integer)
	private int totalAll;
	// 实际
	@Field(type = FieldType.Integer)
	private int act_cnt_up = 0;
	@Field(type = FieldType.Double)
	private double act_percent = 0;// 实际概率
	@Field(type = FieldType.Double)
	private double act_totalProfit = 0;
	// 理论最高总盈亏
	@Field(type = FieldType.Double)
	private double totalProfit = 0.0;
	@Field(type = FieldType.Double)
	private double totalLoss = 0.0;

	@Field(type = FieldType.Text) // 实际被套区间
	private String lossSettAct;
	@Field(type = FieldType.Text) // 理论最低收盘价被套区间
	private String lossSettClosedPrice;
	@Field(type = FieldType.Text)
	private String lossSettLowPrice;// 理论最低价被套区间

	@Field(type = FieldType.Text)
	private String other;

}
