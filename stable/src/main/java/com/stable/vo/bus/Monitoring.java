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
@Document(indexName = "monitoring")
public class Monitoring {
	@Id
	private String code;

	@Field(type = FieldType.Integer)
	private int buy;// 买入监听
	@Field(type = FieldType.Integer)
	private int reqBuyDate;// 请求买人时间
	@Field(type = FieldType.Integer)
	private int lastMoniDate;// 最后监听日期
}
