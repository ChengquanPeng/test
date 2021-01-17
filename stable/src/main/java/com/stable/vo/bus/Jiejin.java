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
@Document(indexName = "jie_jin")
public class Jiejin extends EsBase {
	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int date;// 解禁时间
	@Field(type = FieldType.Long)
	private long num;// 数量
	@Field(type = FieldType.Double)
	private double cost;// 成本
	@Field(type = FieldType.Text)
	private String type;// 解禁股份类型
	@Field(type = FieldType.Double)
	private double zb;// 占比
	@Field(type = FieldType.Double)
	private double zzb;// 总占比
	@Field(type = FieldType.Integer)
	private int sysdate;
	public Jiejin() {

	}

}
