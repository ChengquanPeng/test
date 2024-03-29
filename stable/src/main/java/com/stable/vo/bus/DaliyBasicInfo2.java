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
@Document(indexName = "daliy_basic_info2")
public class DaliyBasicInfo2 extends EsBase {
	private static final long serialVersionUID = 7543493371847034850L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date;
	// 收盘价
	@Field(type = FieldType.Double)
	private double closed;

	// 每日指标
	@Field(type = FieldType.Double)
	private double pe;// float 市盈率（总市值/净利润）
	@Field(type = FieldType.Double)
	private double ped;// float 市盈率（动态）
	@Field(type = FieldType.Double)
	private double peTtm;// float 市盈率（TTM）
	@Field(type = FieldType.Double)
	private double pb;// float 市净率（总市值/净资产）
	@Field(type = FieldType.Double)
	private double totalShare;// float 总股本 （万股）
	@Field(type = FieldType.Double)
	private double floatShare;// float 流通股本 （万股）
	@Field(type = FieldType.Double)
	private double totalMarketVal;// 总市值 （亿）
	@Field(type = FieldType.Double)
	private double circMarketVal;// 流通市值（亿）

	public DaliyBasicInfo2() {

	}

	public DaliyBasicInfo2(JSONArray arr) {
		int i = 0;
		String tscode = arr.getString(i++);// ts_code
		this.code = TushareSpider.removets(tscode);
		this.date = Integer.valueOf(arr.getString(i++));
		arr.getString(i++);// this.open = Double.valueOf();
		arr.getString(i++);// this.high = Double.valueOf(arr.getString(i++));
		arr.getString(i++);// this.low = Double.valueOf(arr.getString(i++));
		this.closed = Double.valueOf(arr.getString(i++));
		// this.yesterdayPrice = Double.valueOf(arr.getString(i++));// pre_close
		// this.todayChange = Double.valueOf(arr.getString(i++));// change
//		this.todayChangeRate = Double.valueOf(arr.getString(i++));// pct_chg
//		this.volume = Double.valueOf((Double.valueOf(arr.getString(i++)) * 100)).longValue();// 成交量 （手）
//		this.amt = Double.valueOf((Double.valueOf(arr.getString(i++)) * 1000)).longValue();// 成交额 （千元）
		setId();

	}

	private void setId() {
		this.id = code + date;
	}

	public DaliyBasicInfo2(String c, int d) {
		this.code = c;
		this.date = d;
		setId();
	}
}
