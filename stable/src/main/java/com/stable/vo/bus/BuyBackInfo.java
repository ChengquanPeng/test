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
@Document(indexName = "buy_back")
public class BuyBackInfo extends EsBase {
//ts_code	str	Y	TS代码
//	ann_date	str	Y	公告日期
//	end_date	str	Y	截止日期
//	proc	str	Y	进度
//	exp_date	str	Y	过期日期
//	vol	float	Y	回购数量
//	amount	float	Y	回购金额
//	high_limit	float	Y	回购最高价
//	low_limit	float	Y	回购最低价

	/**
	 * 
	 */
	private static final long serialVersionUID = 3583785395111756892L;
	/**
	 * 
	 */
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Keyword)
	private String ts_code;
	@Field(type = FieldType.Integer)
	private int ann_date;
	@Field(type = FieldType.Integer)
	private int end_date;
	@Field(type = FieldType.Integer)
	private int exp_date;

	@Field(type = FieldType.Keyword)
	private String proc;
	@Field(type = FieldType.Double)
	private Double vol = 0d;
	@Field(type = FieldType.Double)
	private Double amount = 0d;

	@Field(type = FieldType.Double)
	private Double high_limit = 0d;
	@Field(type = FieldType.Double)
	private Double low_limit = 0d;

	@Field(type = FieldType.Date)
	private Date updDate;

	public BuyBackInfo() {

	}

	public BuyBackInfo(JSONArray arr) {
		int i = 0;
		ts_code = arr.getString(i++);
		this.code = TushareSpider.removets(ts_code);
		try {
			ann_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {

		}
		try {
			ann_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {

		}
		proc = arr.getString(i++);
		try {
			exp_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {

		}
		try {
			this.vol = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.amount = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.high_limit = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			this.low_limit = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		this.updDate = new Date();
		this.id = this.code + ann_date;
	}
}
