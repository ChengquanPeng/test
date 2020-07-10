package com.stable.vo.bus;

import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "code_base_model")
public class CodeBaseModel extends EsBase {
	private static final long serialVersionUID = 1L;

	private String id;
	private String code;
	private int date;
	private int currYear;
	private int currQuarter;
	// ===========基本能力===========
	// 营收(科技类,故事类主要指标)
	private int incomeUpYears;// 年报连续营年数？
	private int incomeUp2yearc;// 年报连续2年营收持续增长？
	private int incomeUpQuartert;// 最近季度同比增长？
	private int incomeUp2quarterc;// 最近2个季度同比持续增长？
	// 利润(传统行业,销售行业主要指标)
	private int profitUpYears;// 年报盈利年数？
	private int profitUp2yearc;// 年报连续2年盈利持续增长？
	private int profitUpQuartert;// 最近季度同比增长？
	private int profitUp2quarterc;// 最近2个季度同比持续增长？

	// ===========现金能力===========
	// 分红
	private int lastDividendDate;// 最近分红年份
	private int dividendCnt;// 最近分红次数（需要参考上市年份）
	// 回购
	private int inBacking;// 正在回购
	private int lastBackDate;// 最后一次回购日期
	private int backCnt;// 累计回购次数

	// ===========主力行为===========

	// ===========地雷===========
	// 利润地雷
	private int profitDownQuarter;// 最近季度利润亏损TODO//科技类，故事类不看此指标
	private int profitDownQuarters;// 最近3个季度连续亏损TODO//科技类，故事类不看此指标
	private int profitDownYears;// 年报连续亏损年数？（可能退市）
	private int profitDownQuartert;// 最近季度同比下降？
	// 营收地雷
	private int incomeDownYear;// 年营收同比下降
	private int incomeDownQuarter;// 季度营收同比下降
	private int incomeDown2Quarter;// 最近2个季度同比扩大下降
	// 分红，回购
	private int noDividend2year;// 最近1年无分红
	private int noBack2year;// 最近1年无分红
	// 质押比例
	private int endDate;// 截止日期
	private double pledgeRatio;// 质押比例
	// 限售股解禁
	private int floatDate;// 解禁日期
	private double floatRatio;// 流通股份占总股本比率

	public String getKeyString() {
		return "[code=" + code + ",currYear=" + currYear + ", currQuarter=" + currQuarter + ", lastDividendDate="
				+ lastDividendDate + ", lastBackDate=" + lastBackDate + ", endDate=" + endDate + ", floatDate="
				+ floatDate + "]";
	}

}
