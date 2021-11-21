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
	private double topThree;// 总股本
	@Field(type = FieldType.Double)
	private double percent5;// 总股本占比
	@Field(type = FieldType.Double)
	private double percent5circZb;// 流通股占比
	@Field(type = FieldType.Integer)
	private int sysdate;

	public void addTop3(double num) {
		topThree += num;
	}

	public void addPercent5(double num) {
		percent5 += num;
	}

	public void cuteTopTotol() {
		topThree = CurrencyUitl.roundHalfUp(topThree);
		percent5 = CurrencyUitl.roundHalfUp(percent5);
	}
}
