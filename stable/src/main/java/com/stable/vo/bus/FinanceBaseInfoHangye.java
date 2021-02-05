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
@Document(indexName = "finance_base_info_hangye")
public class FinanceBaseInfoHangye extends EsBase {
	private static final long serialVersionUID = 2305594280230790360L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	private int year;// 年
	@Field(type = FieldType.Integer)
	private int quarter;// 季度

	@Field(type = FieldType.Text)
	private String hangyeId;
	@Field(type = FieldType.Text)
	private String hangyeName;

	@Field(type = FieldType.Integer)
	private int mllRank;// 毛利排名
	@Field(type = FieldType.Double)
	private double mllAvg;// 毛利平均
	@Field(type = FieldType.Double)
	private double mll;// 毛利平均

	@Field(type = FieldType.Integer)
	private int yszkRank;// 应收账款占比排名
	@Field(type = FieldType.Double)
	private double yszkAvg;// 应收账款占比平均
	@Field(type = FieldType.Double)
	private double yszk;// 应收账款占比

	@Field(type = FieldType.Integer)
	private int xjlRank;// 现金流排名
	@Field(type = FieldType.Double)
	private double xjlAvg;// 现金流平均
	@Field(type = FieldType.Double)
	private double xjl;// 现金流

	@Field(type = FieldType.Integer)
	private int updateDate;
}
