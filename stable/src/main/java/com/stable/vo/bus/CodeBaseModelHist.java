package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "code_base_model_hist")
public class CodeBaseModelHist extends EsBase {
	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	protected int date;
	@Field(type = FieldType.Integer)
	protected int currYear;
	@Field(type = FieldType.Integer)
	protected int currQuarter;
	// ===========基本能力===========
	// 营收(科技类,故事类主要指标)
	@Field(type = FieldType.Integer)
	protected int incomeUpYear;// 年报连续营收持续增长？
	@Field(type = FieldType.Integer)
	protected int incomeUpQuarter;// 最近季度同比增长？
	@Field(type = FieldType.Integer)
	protected int incomeUp2Quarter;// 最近2个季度同比持续增长？
	// 利润(传统行业,销售行业主要指标)
	@Field(type = FieldType.Integer)
	protected int profitUpYear;// 年报持续增长？
	@Field(type = FieldType.Integer)
	protected int profitUpQuarter;// 最近季度同比增长？
	@Field(type = FieldType.Integer)
	protected int profitUp2Quarter;// 最近2个季度同比持续增长？

	// ===========现金能力===========
	// 分红
	@Field(type = FieldType.Integer)
	protected int lastDividendDate;// 最近分红年份
	// 回购
	@Field(type = FieldType.Integer)
	protected int lastBackDate;// 最后一次回购日期

	// ===========主力行为===========

	// ===========地雷===========
	// 营收地雷
	@Field(type = FieldType.Integer)
	protected int incomeDownYear;// 年营收同比下降
	@Field(type = FieldType.Integer)
	protected int incomeDownQuarter;// 季度营收同比下降
	@Field(type = FieldType.Integer)
	protected int incomeDown2Quarter;// 最近2个季度同比扩大下降
	// 利润地雷
	@Field(type = FieldType.Integer)
	protected int profitDownYear;// 年净利同比下降 //科技类，故事类不看此指标
	@Field(type = FieldType.Integer)
	protected int profitDownQuarter;// 季度净利同比下降 //科技类，故事类不看此指标
	@Field(type = FieldType.Integer)
	protected int profitDown2Quarter;// 最近2个季度连续下降TODO//科技类，故事类不看此指标
	@Field(type = FieldType.Integer)
	protected int profitDown2Year;// 年报连续亏损年数？（可能退市）

	// 分红，回购
	@Field(type = FieldType.Integer)
	protected int noDividendyear;// 最近1年无分红
	@Field(type = FieldType.Integer)
	protected int noBackyear;// 最近1年无回购
	// 质押比例
	@Field(type = FieldType.Integer)
	protected int endDate;// 截止日期
	@Field(type = FieldType.Double)
	protected double pledgeRatio;// 质押比例
	// 限售股解禁
	@Field(type = FieldType.Integer)
	protected int floatDate;// 解禁日期
	@Field(type = FieldType.Double)
	protected double floatRatio;// 流通股份占总股本比率

	// 业绩快报预告
	@Field(type = FieldType.Integer)
	protected int forestallYear;
	@Field(type = FieldType.Integer)
	protected int forestallQuarter;

	// 增长
	@Field(type = FieldType.Integer)
	protected double forestallIncomeTbzz;// 营收增长
	@Field(type = FieldType.Integer)
	protected double forestallProfitTbzz;// 利润增长
	@Field(type = FieldType.Integer)
	protected double currIncomeTbzz;// 营收增长
	@Field(type = FieldType.Integer)
	protected double currProfitTbzz;// 利润增长

	// 分数
	@Field(type = FieldType.Integer)
	private int score = 0;
	// 提高分数
	@Field(type = FieldType.Integer)
	private int upScore = 0;
	@Field(type = FieldType.Integer)
	private int udpateDate;

	public String getKeyString() {
		return "[code=" + code + ",currYear=" + currYear + ", currQuarter=" + currQuarter + ", lastDividendDate="
				+ lastDividendDate + ", lastBackDate=" + lastBackDate + ", endDate=" + endDate + ", floatDate="
				+ floatDate + ", forestallYear=" + forestallYear + ", forestallQuarter=" + forestallQuarter + "]";
	}

}
