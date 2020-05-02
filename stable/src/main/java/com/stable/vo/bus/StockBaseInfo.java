package com.stable.vo.bus;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "stock_base_info")
public class StockBaseInfo extends EsBase{
//	ts_code	str	TS代码
//	symbol	str	股票代码
//	name	str	股票名称
//	area	str	所在地域
//	industry	str	所属行业
//	fullname	str	股票全称
//	enname	str	英文全称
//	market	str	市场类型 （主板/中小板/创业板）
//	exchange	str	交易所代码
//	curr_type	str	交易货币
//	list_status	str	上市状态： L上市 D退市 P暂停上市
//	list_date	str	上市日期
//	delist_date	str	退市日期
//	is_hs	str	是否沪深港通标的，N否 H沪股通 S深股通

	/**
	 * 
	 */
	private static final long serialVersionUID = 4910896688001534666L;
	@Id
	private String code;
	@Field(type = FieldType.Text)
	private String ts_code;
	@Field(type = FieldType.Text)
	private String name;
	@Field(type = FieldType.Text)
	private String area;
	@Field(type = FieldType.Text)
	private String industry;
	@Field(type = FieldType.Text)
	private String fullname;
	@Field(type = FieldType.Text)
	private String enname;
	@Field(type = FieldType.Text)
	private String market;
	@Field(type = FieldType.Text)
	private String exchange;
	@Field(type = FieldType.Text)
	private String curr_type;
	@Field(type = FieldType.Text)
	private String list_status;
	@Field(type = FieldType.Text)
	private String list_date;
	@Field(type = FieldType.Text)
	private String delist_date;
	@Field(type = FieldType.Text)
	private String is_hs;
	@Field(type = FieldType.Long)
	private Date updDate;

	public StockBaseInfo() {

	}

	public StockBaseInfo(JSONArray arr) {
		int i = 0;
		ts_code = arr.getString(i++);
		code = arr.getString(i++);
		name = arr.getString(i++);
		area = arr.getString(i++);
		industry = arr.getString(i++);
		fullname = arr.getString(i++);
		enname = arr.getString(i++);
		market = arr.getString(i++);
		exchange = arr.getString(i++);
		curr_type = arr.getString(i++);
		list_status = arr.getString(i++);
		list_date = arr.getString(i++);
		delist_date = arr.getString(i++);
		is_hs = arr.getString(i++);
		this.updDate = new Date();
	}
}
