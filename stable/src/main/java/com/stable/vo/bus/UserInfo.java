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
@Document(indexName = "user_info")
public class UserInfo extends EsBase {
	private static final long serialVersionUID = 1L;

	@Id
	@Field(type = FieldType.Long)
	private long id;// 手机号

	@Field(type = FieldType.Text)
	private String name;// 名字

	@Field(type = FieldType.Text)
	private String wxpush;// 微信推送id

	@Field(type = FieldType.Integer)
	private int type;// 类型：1管理员，2普通

	@Field(type = FieldType.Integer)
	private int s1;// 服务1过期时间

	@Field(type = FieldType.Integer)
	private int s2;// 服务2过期时间

	@Field(type = FieldType.Text)
	private String remark;// 备注

	@Field(type = FieldType.Integer)
	private int createDate;// 加入日期

	@Field(type = FieldType.Integer)
	private int memDate;// 会员日期
}
