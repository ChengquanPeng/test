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
@Document(indexName = "pledge_stat")
public class PledgeStat extends EsBase {
	private static final long serialVersionUID = -6862288561607105101L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int endDate;// 截止日期
	@Field(type = FieldType.Integer)
	private int pledgeCount;// 质押次数
	@Field(type = FieldType.Double)
	private double unrestPledge;// 无限售股质押数量（万）
	@Field(type = FieldType.Double)
	private double restPledge;// 限售股份质押数量（万）
	@Field(type = FieldType.Double)
	private double totalShare;// 总股本
	@Field(type = FieldType.Double)
	private double pledgeRatio;// 质押比例

	public PledgeStat() {
		
	}
	// ts_code,end_date,pledge_count,unrest_pledge,rest_pledge,total_share,pledge_ratio
	public PledgeStat(JSONArray arr) {
		int i = 0;
		String ts_code = arr.getString(i++);
		this.code = TushareSpider.removets(ts_code);
		endDate = arr.getIntValue(i++);
		pledgeCount = arr.getIntValue(i++);
		unrestPledge = arr.getDoubleValue(i++);
		restPledge = arr.getDoubleValue(i++);
		totalShare = arr.getDoubleValue(i++);
		pledgeRatio = arr.getDoubleValue(i++);

		id = code + endDate;
	}
}
