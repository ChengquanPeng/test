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
@Document(indexName = "trade_hist_info_daliy_nofq")
public class TradeHistInfoDaliyNofq extends EsBase {

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

	public TradeHistInfoDaliyNofq() {

	}

	public TradeHistInfoDaliyNofq(JSONArray arr) {
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

//	日期，		开盘，收盘，最高，最低，总手，总额，					振幅，	涨跌幅，		涨跌额，	换手，
//	2020-09-18,	31.26,31.02,31.60,30.64,64224,198563405.00,	3.07,	-0.83,		-0.26,	0.80
//	2020-09-21,	31.12,30.38,31.13,29.92,72290,218674388.00,	3.90,	-2.06,		-0.64,	0.90
	public TradeHistInfoDaliyNofq(String code, String lineFromEastMoeny) {
		this.code = code;
		String[] vals = lineFromEastMoeny.split(",");
		int i = 0;
		this.date = Integer.valueOf(vals[i++].replaceAll("-", ""));// TODO
		this.open = Double.valueOf(vals[i++]);
		this.closed = Double.valueOf(vals[i++]);
		this.high = Double.valueOf(vals[i++]);
		this.low = Double.valueOf(vals[i++]);
		this.volume = Double.valueOf((Double.valueOf(vals[i++]) * 100)).longValue();// 成交量 （手）
		this.amt = Double.valueOf((Double.valueOf(vals[i++]))).longValue();// 成交额 （千元）
		i++;// 振幅
		this.todayChangeRate = Double.valueOf(vals[i++]);// pct_chg涨跌幅
		this.todayChange = Double.valueOf(vals[i++]);// change 涨跌额
//		this.yesterdayPrice = Double.valueOf(arr.getString(i++));// pre_close
		setId();
	}

	public void setId() {
		this.id = code + date;
	}
}
