package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;
import com.stable.spider.tushare.TushareSpider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "share_float")
public class ShareFloat extends EsBase {
	private static final long serialVersionUID = -6862288561607105101L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int annDate;// 公告日期
	@Field(type = FieldType.Integer)
	private int floatDate;// 解禁日期
	@Field(type = FieldType.Double)
	private double floatShare;// 流通股份
	@Field(type = FieldType.Double)
	private double floatRatio;// 流通股份占总股本比率
	@Field(type = FieldType.Text)
	private String shareType;// 股份类型

	// ts_code,ann_date,float_date,float_share,float_ratio,holder_name,share_type
	public ShareFloat() {
		
	}
	public ShareFloat(JSONArray arr) {
		int i = 0;
		String ts_code = arr.getString(i++);
		this.code = TushareSpider.removets(ts_code);
		annDate = arr.getIntValue(i++);
		floatDate = arr.getIntValue(i++);
		floatShare = arr.getDoubleValue(i++);
		floatRatio = arr.getDoubleValue(i++);
		arr.getString(i++);
		shareType = arr.getString(i++);
		id = code + annDate;
	}
}
