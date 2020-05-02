package com.stable.vo.bus;

import java.util.Date;

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
@Document(indexName = "dividend_history")
public class DividendHistory extends EsBase {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4881034024969869338L;

	// code+date
	@Id
	private String id;
	// code
	@Field(type = FieldType.Text)
	private String code;
	// TS代码
	@Field(type = FieldType.Text)
	private String ts_code;
	// 分红年度
	@Field(type = FieldType.Integer)
	private int end_date;
	// 预案公告日
	@Field(type = FieldType.Integer)
	private int ann_date;
	// 实施进度
	@Field(type = FieldType.Text)
	private String div_proc;
	// 每股送转
	@Field(type = FieldType.Text)
	private double stk_div;
	// 每股送股比例
	@Field(type = FieldType.Double)
	private double stk_bo_rate = 0d;
	// 每股转增比例
	@Field(type = FieldType.Double)
	private double stk_co_rate = 0d;
	// 每股分红（税后）
	@Field(type = FieldType.Double)
	private double cash_div = 0d;
	// 每股分红（税前）
	@Field(type = FieldType.Double)
	private double cash_div_tax = 0d;
	// 股权登记日
	@Field(type = FieldType.Integer)
	private int record_date = 0;
	// 除权除息日
	@Field(type = FieldType.Integer)
	private int ex_date = 0;
	// 派息日
	@Field(type = FieldType.Integer)
	private int pay_date = 0;
	// 红股上市日
	@Field(type = FieldType.Integer)
	private int div_listdate = 0;
	// 基准日
	@Field(type = FieldType.Integer)
	private int base_date = 0;
	// 基准股本（万）
	@Field(type = FieldType.Text)
	private String base_share;
	// time
	@Field(type = FieldType.Long)
	private Date updDate;

	public DividendHistory() {

	}

	public DividendHistory(JSONArray arr) {
		int i = 0;
		this.ts_code = arr.getString(i++);// ts_code
		this.code = TushareSpider.removets(ts_code);
		this.end_date = Integer.valueOf(arr.getString(i++));
		try {
			this.ann_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		this.div_proc = arr.getString(i++);
		try {
			this.stk_div = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.stk_bo_rate = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.stk_co_rate = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.cash_div = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.cash_div_tax = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.record_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.ex_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.pay_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.div_listdate = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		arr.getString(i++);//// 实施公告日
		this.updDate = new Date();
		setId();
	}

	private void setId() {
		this.id = code + ann_date + div_proc;
	}
}
