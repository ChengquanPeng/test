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

	/**
	 * 
	 */
	private static final long serialVersionUID = 3583785305211756892L;
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
	@Field(type = FieldType.Integer)
	private int sysdate;

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
