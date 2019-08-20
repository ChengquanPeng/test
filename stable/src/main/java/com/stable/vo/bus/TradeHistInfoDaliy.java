package com.stable.vo.bus;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.Constant;
import com.stable.es.vo.EsBase;

import lombok.Data;

@Data
@Document(indexName = "trade_hist_info_daliy")
public class TradeHistInfoDaliy extends EsBase {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

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
	// 交易量(股)
	@Field(type = FieldType.Long)
	private long volume;
	// 交易金额(元)
	@Field(type = FieldType.Long)
	private long amt;
	@Field(type = FieldType.Long)
	private Date updDate;

	public TradeHistInfoDaliy() {

	}

	public TradeHistInfoDaliy(String code, String line) {
		String[] f = line.split(Constant.DOU_HAO);
		String d = f[0];
		this.date = Integer.valueOf(d.replaceAll(Constant.SYMBOL_, Constant.EMPTY_STRING));
		this.code = code;
		this.open = Double.valueOf(f[1]);
		this.high = Double.valueOf(f[2]);
		this.closed = Double.valueOf(f[3]);
		this.low = Double.valueOf(f[4]);
		this.volume = Long.valueOf(f[5]);
		this.amt = Long.valueOf(f[6]);
		this.updDate = new Date();
		setId();
	}

	public TradeHistInfoDaliy(String code, JSONArray arr) {
		int i = 0;
		arr.getString(i++);// ts_code
		this.code = code;
		this.date = Integer.valueOf(arr.getString(i++));
		this.open = Double.valueOf(arr.getString(i++));
		this.high = Double.valueOf(arr.getString(i++));
		this.low = Double.valueOf(arr.getString(i++));
		this.closed = Double.valueOf(arr.getString(i++));
		arr.getString(i++);// pre_close
		arr.getString(i++);// change
		arr.getString(i++);// pct_chg
		this.volume = Double.valueOf((Double.valueOf(arr.getString(i++)) * 100)).longValue();// 成交量 （手）
		this.amt = Double.valueOf((Double.valueOf(arr.getString(i++)) * 1000)).longValue();// 成交额 （千元）
		this.updDate = new Date();
		setId();
	}
	
	private void setId() {
		this.id = code + date;
	}
}
