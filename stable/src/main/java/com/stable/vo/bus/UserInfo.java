package com.stable.vo.bus;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.stable.constant.Constant;
import com.stable.msg.MsgPushServer;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
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

	@Field(type = FieldType.Text)
	private String lastLogin;// 最后登录

	public boolean getPushWay() {
		if (id == Constant.MY_ID) {
			wxpush = MsgPushServer.email.myId;
			return true;
		} else if (StringUtils.isNotBlank(wxpush)) {
			return false;
		} else {
			wxpush = id + MsgPushServer.qqmail;
			return true;
		}
	}
}
