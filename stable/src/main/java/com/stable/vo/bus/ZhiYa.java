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
@Document(indexName = "zhi_ya")
public class ZhiYa extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int update;// 更新日期
	@Field(type = FieldType.Integer)
	private int hasRisk;// 是否有风险(0-1)
	@Field(type = FieldType.Double)
	private double totalRatio;// 总质押比例
	@Field(type = FieldType.Double)
	private double highRatio;// 最高质押比例
	@Field(type = FieldType.Text)
	private String detail;// 质押明细

	@Field(type = FieldType.Double)
	private double warningLine = 0.0;
	@Field(type = FieldType.Double)
	private double openLine = 0.0;

	public ZhiYa() {

	}

}
