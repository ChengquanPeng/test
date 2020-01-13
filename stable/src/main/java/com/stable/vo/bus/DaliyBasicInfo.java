package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;
import com.stable.spider.tushare.TushareSpider;

import lombok.Data;

@Data
@Document(indexName = "daliy_basic_info")
public class DaliyBasicInfo extends EsBase {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	// code+date
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;// 股票代码
	@Field(type = FieldType.Text)
	private String ts_code;// str TS股票代码
	@Field(type = FieldType.Integer)
	private int trade_date;// str 交易日期
	@Field(type = FieldType.Double)
	private double close;// float 当日收盘价
	@Field(type = FieldType.Double)
	private double turnover_rate;// float 换手率（%）
	@Field(type = FieldType.Double)
	private double turnover_rate_f;// float 换手率（自由流通股）
	@Field(type = FieldType.Double)
	private double volume_ratio;// float 量比
	@Field(type = FieldType.Double)
	private double pe;// float 市盈率（总市值/净利润）
	@Field(type = FieldType.Double)
	private double pe_ttm;// float 市盈率（TTM）
	@Field(type = FieldType.Double)
	private double pb;// float 市净率（总市值/净资产）
	@Field(type = FieldType.Double)
	private double ps;// float 市销率
	@Field(type = FieldType.Double)
	private double ps_ttm;// float 市销率（TTM）
	@Field(type = FieldType.Double)
	private double total_share;// float 总股本 （万股）
	@Field(type = FieldType.Double)
	private double float_share;// float 流通股本 （万股）
	@Field(type = FieldType.Double)
	private double free_share;// float 自由流通股本 （万）
	@Field(type = FieldType.Double)
	private double total_mv;// float 总市值 （万元）
	@Field(type = FieldType.Double)
	private double circ_mv;// float 流通市值（万元）

	@Field(type = FieldType.Double)
	private double dv_ratio;// 股息率 （%）
	@Field(type = FieldType.Double)
	private double dv_ttm;// 股息率（TTM）（%）

	public DaliyBasicInfo() {

	}

	public DaliyBasicInfo(JSONArray arr) {

		int i = 0;
		this.ts_code = arr.getString(i++);// ts_code
		this.code = TushareSpider.removets(ts_code);
		this.trade_date = Integer.valueOf(arr.getString(i++));
		try {
			this.close = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.turnover_rate = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.turnover_rate_f = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.volume_ratio = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.pe = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.pe_ttm = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.pb = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.ps = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.ps_ttm = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.dv_ratio = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.dv_ttm = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.total_share = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.float_share = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.free_share = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.total_mv = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.circ_mv = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		setId();
	}

	private void setId() {
		this.id = code + trade_date;
	}
}
