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
@Document(indexName = "finance_base_info2")
public class FinanceBaseInfo extends EsBase {
	private static final long serialVersionUID = 2305594280230790359L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int date; // 报告日期
	@Field(type = FieldType.Integer)
	private Integer annDate = 0; // 公告日期
	@Field(type = FieldType.Integer)
	private int year;// 年
	@Field(type = FieldType.Integer)
	private int quarter;// 季度

	@Field(type = FieldType.Long)
	private Long yyzsr; // 营业总收入
	@Field(type = FieldType.Long)
	private Long gsjlr; // 归属净利润
	@Field(type = FieldType.Long)
	private Long kfjlr; // 扣非净利润

	@Field(type = FieldType.Double)
	private double yyzsrtbzz; // 营业总收入同比增长(%)
	@Field(type = FieldType.Double)
	private double gsjlrtbzz; // 归属净利润同比增长(%)
	@Field(type = FieldType.Double)
	private double kfjlrtbzz; // 扣非净利润同比增长(%)

	@Field(type = FieldType.Double)
	private double jqjzcsyl; // 加权净资产收益率(%) -加权净资产收益率=当期净利润/当期加权平均净资产
	@Field(type = FieldType.Double)
	private double tbjzcsyl; // 摊薄净资产收益率(%) -摊薄净资产收益率=报告期净利润/期末净资产
	@Field(type = FieldType.Double)
	private double mgjyxjl; // 每股经营现金流

	public FinanceBaseInfo() {

	}

	public FinanceBaseInfo(String code, int date) {
		this.code = code;
		this.date = date;
		String datestr = String.valueOf(date);
		this.year = Integer.valueOf(datestr.substring(0, 4));
		int m = Integer.valueOf(datestr.substring(4, 6));
		if (m == 12) {
			this.quarter = 4;
		} else if (m == 9) {
			this.quarter = 3;
		} else if (m == 6) {
			this.quarter = 2;
		} else if (m == 3) {
			this.quarter = 1;
		} else {
			this.quarter = 0;
		}
		this.id = this.code + "_" + this.date;
	}
}
