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
@Document(indexName = "user_amt_log")
public class UserAmtLog extends EsBase {
	private static final long serialVersionUID = 1L;
	@Id
	private String logId;// 手机号
	@Field(type = FieldType.Integer)
	private int uid;// 用户id
	@Field(type = FieldType.Integer)
	private int date;// 日期
	@Field(type = FieldType.Integer)
	private int stype;// 充值产品类型
	@Field(type = FieldType.Double)
	private double amt;// 充值金额
	@Field(type = FieldType.Integer)
	private int expiredDate;// 产品类型过期日期
}
