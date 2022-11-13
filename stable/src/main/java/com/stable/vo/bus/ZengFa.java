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
@Document(indexName = "zeng_fa")
public class ZengFa extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String id;// (code+startdate+yjamt)
	@Field(type = FieldType.Keyword)
	private String code;

	@Field(type = FieldType.Integer)
	private int status;// 1开始，2完成，3作废
	@Field(type = FieldType.Keyword)
	private String statusDesc;// 进度
	@Field(type = FieldType.Keyword)
	private String issueClz;// 发行类型
	@Field(type = FieldType.Keyword)
	private String issueType;// 发行方式

	// 日期
	@Field(type = FieldType.Integer)
	private int startDate;// 董事会公告日
	@Field(type = FieldType.Integer)
	private int zjhDate;// 证监会核准公告日
	@Field(type = FieldType.Integer)
	private int numOnLineDate;// 发行新股日
	@Field(type = FieldType.Integer)
	private int endDate;// 新股上市公告日

	// 价格&数量
	@Field(type = FieldType.Double)
	private double price;// 实际发行价格：17.0200元
	@Field(type = FieldType.Text)
	private String num;// 实际发行数量：1941.87万股
	@Field(type = FieldType.Text)
	private String amt;// 实际募资净额：3.31亿元
	@Field(type = FieldType.Long)
	private long yjamt = 0;// 预计募资净额

	@Field(type = FieldType.Integer)
	private int update;

	public ZengFa() {

	}

}
