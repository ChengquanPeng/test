package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;
import com.stable.spider.tushare.TushareSpider;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "trade_hist_info_daliy")
public class TradeHistInfoDaliy extends EsBase {

	private static final long serialVersionUID = 3409627904929839927L;
	// code+date
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	// 日期date yyyymmdd
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

	public TradeHistInfoDaliy() {

	}

	public TradeHistInfoDaliy(JSONArray arr) {
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

	public void setId() {
		this.id = code + date;
	}
}
