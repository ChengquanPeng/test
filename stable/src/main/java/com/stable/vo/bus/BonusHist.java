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
@Document(indexName = "bonus_hist")
public class BonusHist extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int rptDate;// 董事会日期
	@Field(type = FieldType.Text)
	private String rptYear;// 报告期
	@Field(type = FieldType.Keyword)
	private String status;// 进度
	@Field(type = FieldType.Text)
	private String detail;// 分红方案说明
	@Field(type = FieldType.Integer)
	private int bookDate;// 股权登记日
	@Field(type = FieldType.Integer)
	private int dividendDate;// A股除权除息日
	@Field(type = FieldType.Text)
	private String amt;// 实际募资净额：3.31亿元
	@Field(type = FieldType.Integer)
	private int update;
	@Field(type = FieldType.Integer)
	private int hasZhuanGu;//是否转股
	
	public BonusHist() {

	}

}
