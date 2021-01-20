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
	@Field(type = FieldType.Text)
	private String code;
	// 日期date
	@Field(type = FieldType.Integer)
	private int date;
	// 开盘价
	@Field(type = FieldType.Double)
	private double open;
	// 最高价
	@Field(type = FieldType.Double)
	private double high;
	// 收盘价
	@Field(type = FieldType.Double)
	private double closed;
	// 最低价
	@Field(type = FieldType.Double)
	private double low;
	// 交易量(手)
	@Field(type = FieldType.Double)
	private double volume;
	// 交易金额(千元)
	@Field(type = FieldType.Double)
	private double amt;
	// 昨收
	@Field(type = FieldType.Double)
	private double yesterdayPrice;
	// 今日涨跌额
	@Field(type = FieldType.Double)
	private double todayChange;
	// 今日涨跌幅
	@Field(type = FieldType.Double)
	private double todayChangeRate;

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
	private double totalMarketVal;// float 总市值 （万元）
	@Field(type = FieldType.Double)
	private double circMarketVal;// float 流通市值（万元）
	@Field(type = FieldType.Double)
	private double szl;// float 市赚率 市盈率/净资产收益率（PE/ROE）

	public DaliyBasicInfo2() {

	}

	public DaliyBasicInfo2(JSONArray arr) {
		int i = 0;
		String tscode = arr.getString(i++);// ts_code
		this.code = TushareSpider.removets(tscode);
		this.date = Integer.valueOf(arr.getString(i++));
		this.open = Double.valueOf(arr.getString(i++));
		this.high = Double.valueOf(arr.getString(i++));
		this.low = Double.valueOf(arr.getString(i++));
		this.closed = Double.valueOf(arr.getString(i++));
		this.yesterdayPrice = Double.valueOf(arr.getString(i++));// pre_close
		this.todayChange = Double.valueOf(arr.getString(i++));// change
		this.todayChangeRate = Double.valueOf(arr.getString(i++));// pct_chg
		this.volume = Double.valueOf((Double.valueOf(arr.getString(i++)) * 100)).longValue();// 成交量 （手）
		this.amt = Double.valueOf((Double.valueOf(arr.getString(i++)) * 1000)).longValue();// 成交额 （千元）
		setId();

	}

	private void setId() {
		this.id = code + date;
	}
}
