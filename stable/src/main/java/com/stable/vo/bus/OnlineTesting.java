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
@Document(indexName = "online_testing")
public class OnlineTesting extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Integer)
	private int prd = 0;// 产品类型
	@Field(type = FieldType.Integer)
	private int prdsub;// 产品细分类

	@Field(type = FieldType.Integer)
	private int stat;// 状态：是否已买入
	@Field(type = FieldType.Double)
	private double costPrice;// 成本价(多次买入后的成本价)
	@Field(type = FieldType.Integer)
	private int vol = 0;// 最新持仓量
	@Field(type = FieldType.Integer)
	private int canSold = 0;// 可卖数量
	@Field(type = FieldType.Integer)
	private int buyToday = 0;// 今天已买入
	@Field(type = FieldType.Double)
	private double profitPct;// 收益(百分比)
	@Field(type = FieldType.Double)
	private double profitAmt;// 收益(绝对值)

	@Field(type = FieldType.Integer)
	private int times = 0;// 买入次数
	@Field(type = FieldType.Double)
	private double cost1st;// 第一次买入量：用于计算盈利百分比
	@Field(type = FieldType.Text)
	private String hist;// 买入历史

	public void setIdkey() {
		id = code + "|" + date + "|" + prd;
	}
}
