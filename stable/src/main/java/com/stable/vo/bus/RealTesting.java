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
@Document(indexName = "real_testing")
public class RealTesting {
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date = 0;
	@Field(type = FieldType.Integer)
	private int type = 0;// 测试类型

	@Field(type = FieldType.Double)
	private double buyPrice;// 买入价格
	@Field(type = FieldType.Double)
	private double soldPrice;// 卖出价格
	@Field(type = FieldType.Double)
	private double profit;// 收益
	@Field(type = FieldType.Integer)
	private int sta = 1;// 状态：1买入，2卖出

	public void setId() {
		id = code + "|" + date + "|" + type;
	}
}
