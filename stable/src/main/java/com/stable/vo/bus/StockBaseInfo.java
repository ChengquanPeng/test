package com.stable.vo.bus;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "stock_base_info")
public class StockBaseInfo extends EsBase {
	private static final long serialVersionUID = 4910896688001534666L;
	// 股票代码
	@Id
	private String code;
	@Field(type = FieldType.Text)
	private String ts_code;
	// 股票名称
	@Field(type = FieldType.Text)
	private String name;
	// 同花顺行业
	@Field(type = FieldType.Text)
	private String thsIndustry;
	// 同花顺-主营
	@Field(type = FieldType.Text)
	private String thsMainBiz;
	// 同花顺-亮点
	@Field(type = FieldType.Text)
	private String thsLightspot;
	// 地区
	@Field(type = FieldType.Text)
	private String area;
	// 所属行业-细分行业
	@Field(type = FieldType.Text)
	private String industry;
	// 全称
	@Field(type = FieldType.Text)
	private String fullname;
	// 英文全称
	@Field(type = FieldType.Text)
	private String enname;
	// 市场类型 （主板/中小板/创业板/科创板/CDR）
	@Field(type = FieldType.Text)
	private String market;
	// 交易所代码
	@Field(type = FieldType.Text)
	private String exchange;
	// 交易货币
	@Field(type = FieldType.Text)
	private String curr_type;
	// 上市状态： L上市 D退市 P暂停上市
	@Field(type = FieldType.Text)
	private String list_status;
	// 上市日期
	@Field(type = FieldType.Text)
	private String list_date;
	// 退市日期
	@Field(type = FieldType.Text)
	private String delist_date;
	// 是否沪深港通标的，N否 H沪股通 S深股通
	@Field(type = FieldType.Text)
	private String is_hs;
	@Field(type = FieldType.Long)
	private Date updDate;
	@Field(type = FieldType.Long)
	private Long updBatchNo;
	@Field(type = FieldType.Text)
	private String oldName;// 曾用名
	@Field(type = FieldType.Text)
	private String webSite;// 网站
	

	public StockBaseInfo() {

	}

	public StockBaseInfo(JSONArray arr, Long updBatchNo) {
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
		this.updBatchNo = updBatchNo;
	}
}
