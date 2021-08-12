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
@Document(indexName = "zhi_ya_detail")
public class ZhiYaDetail extends EsBase {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;// code+hash;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Text)
	private String holderName;// 质押股东
	@Field(type = FieldType.Text)
	private String pfOrg;// 质押方
	@Field(type = FieldType.Long)
	private long num;// 质押数量
	@Field(type = FieldType.Integer)
	private int state;// 状态-1已解压,2,未达预警线,3已达预警线，4.其他
	@Field(type = FieldType.Text)
	private String stateDesc;// 状态-1已解压,2,未达预警线,3已达预警线，4.其他
	@Field(type = FieldType.Double)
	private double selfRatio;// 占自己所持比例
	@Field(type = FieldType.Double)
	private double totalRatio;// 占总股本比例
	@Field(type = FieldType.Double)
	private double closePrice;// 质押收盘价
	@Field(type = FieldType.Double)
	private double openline;// 平仓线
	@Field(type = FieldType.Double)
	private double warningLine;// 预警线
	@Field(type = FieldType.Integer)
	private int noticeDate;// 公告日期
	@Field(type = FieldType.Integer)
	private int startDate;// 开始日期
	@Field(type = FieldType.Integer)
	private int unfreezeDate;// 结束日期
	@Field(type = FieldType.Text)
	private String purpose;// 目的

	public ZhiYaDetail() {

	}

}
