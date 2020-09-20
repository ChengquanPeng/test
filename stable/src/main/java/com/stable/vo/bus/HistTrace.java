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
	private int oneYear;// 1年未大涨

	// 结果
	// 盈亏概率
	@Field(type = FieldType.Integer)
	int cnt_up = 0;
	@Field(type = FieldType.Integer)
	int c_m5 = 0;
	@Field(type = FieldType.Integer)
	int c_r5_10 = 0;
	@Field(type = FieldType.Integer)
	int c_m10 = 0;
	@Field(type = FieldType.Double)
	double t_m5 = 0;
	@Field(type = FieldType.Double)
	double t_r5_10 = 0;
	@Field(type = FieldType.Double)
	double t_m10 = 0;

	// 盈亏总额
	@Field(type = FieldType.Integer)
	int cnt_down = 0;
	@Field(type = FieldType.Integer)
	int d_c_m5 = 0;
	@Field(type = FieldType.Integer)
	int d_c_r5_10 = 0;
	@Field(type = FieldType.Integer)
	int d_c_m10 = 0;
	@Field(type = FieldType.Double)
	double d_t_m5 = 0;
	@Field(type = FieldType.Double)
	double d_t_r5_10 = 0;
	@Field(type = FieldType.Double)
	double d_t_m10 = 0;

	// 理论最高总盈亏
	@Field(type = FieldType.Double)
	double totalProfit = 0.0;
	@Field(type = FieldType.Double)
	double totalLoss = 0.0;
	// 实际
	@Field(type = FieldType.Integer)
	int act_cnt_up = 0;
	@Field(type = FieldType.Double)
	double act_totalProfit = 0;
}
