package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.stable.utils.CurrencyUitl;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "holder_percent")
public class HolderPercent extends EsBase {
//ts_code	str	Y	TS代码
//	ann_date	str	Y	公告日期
//	end_date	str	Y	截止日期
//	proc	str	Y	进度
//	exp_date	str	Y	过期日期
//	vol	float	Y	回购数量
//	amount	float	Y	回购金额
//	high_limit	float	Y	回购最高价
//	low_limit	float	Y	回购最低价

	/**
	 * 
	 */
	private static final long serialVersionUID = 3583785305111756892L;
	/**
	 * 
	 */
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int date;
	@Field(type = FieldType.Double)
	private double topThree;
	@Field(type = FieldType.Double)
	private double numOne;
	@Field(type = FieldType.Double)
	private double numTwo;
	@Field(type = FieldType.Double)
	private double numThree;

	public void addNum(double num) {
		if (numOne <= 0) {
			numOne = num;
		} else if (numTwo <= 0) {
			numTwo = num;
		} else if (numThree <= 0) {
			numThree = num;
		}
	}

	public void cuteTopTotol() {
		topThree = CurrencyUitl.roundHalfUp(numOne + numTwo + numThree);
	}
}
