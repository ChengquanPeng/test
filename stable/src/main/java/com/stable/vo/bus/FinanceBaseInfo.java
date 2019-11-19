package com.stable.vo.bus;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;
import com.stable.utils.CurrencyUitl;

import lombok.Data;

@Data
@Document(indexName = "finance_base_info")
public class FinanceBaseInfo extends EsBase {
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;

	// 时间
	@Field(type = FieldType.Integer)
	private int reportDate;

	@Field(type = FieldType.Integer)
	private int year;

	@Field(type = FieldType.Integer)
	private int quarter;

//  基本每股收益
	@Field(type = FieldType.Text)
	private String earningsPerShare;

//	净利润(元)
	@Field(type = FieldType.Long)
	private Long retainedProfits;

//	净利润同比增长率
	@Field(type = FieldType.Text)
	private String retainedProfitsTbRate;

//	扣非净利润(元)
	@Field(type = FieldType.Long)
	private Long retainedProfitsReal;

//	扣非净利润同比增长率
	@Field(type = FieldType.Text)
	private String retainedProfitsRealTbRate;

//	营业总收入(元)
	@Field(type = FieldType.Long)
	private Long income;

//	营业总收入同比增长率
	@Field(type = FieldType.Text)
	private String incomeTbRate;

//	每股净资产
	@Field(type = FieldType.Text)
	private String assetPerShare;

//	净资产收益率
	@Field(type = FieldType.Text)
	private String assetEarningseRate;

//	净资产收益率-摊薄
	@Field(type = FieldType.Text)
	private String assetEarningseRateDilute;

//	资产负债比率
	@Field(type = FieldType.Text)
	private String assetDebtRate;

//	每股资本公积金(元)
	@Field(type = FieldType.Text)
	private String fundPerShare;

//	每股未分配利润(元)
	@Field(type = FieldType.Text)
	private String unpaidProfitsPerShare;

//	每股经营现金流
	@Field(type = FieldType.Text)
	private String cashPerShare;

//	销售净利率 
	@Field(type = FieldType.Text)
	private String profitOnSales;

	@Field(type = FieldType.Date)
	private Date updateDate;

	public void setValue(String code, int index, List<JSONArray> list) {
		this.code = code;
		int indexlist = 0;
		String datestr = list.get(indexlist++).getString(index);
		this.earningsPerShare = list.get(indexlist++).getString(index);
		this.retainedProfits = CurrencyUitl.covertToLong(list.get(indexlist++).getString(index));
		this.retainedProfitsTbRate = list.get(indexlist++).getString(index);
		this.retainedProfitsReal = CurrencyUitl.covertToLong(list.get(indexlist++).getString(index));
		this.retainedProfitsRealTbRate = list.get(indexlist++).getString(index);
		this.income = CurrencyUitl.covertToLong(list.get(indexlist++).getString(index));
		this.incomeTbRate = list.get(indexlist++).getString(index);
		this.assetPerShare = list.get(indexlist++).getString(index);
		this.assetEarningseRate = list.get(indexlist++).getString(index);
		this.assetEarningseRateDilute = list.get(indexlist++).getString(index);
		this.assetDebtRate = list.get(indexlist++).getString(index);
		this.fundPerShare = list.get(indexlist++).getString(index);
		this.unpaidProfitsPerShare = list.get(indexlist++).getString(index);
		this.cashPerShare = list.get(indexlist++).getString(index);
		this.profitOnSales = list.get(indexlist++).getString(index);
		this.updateDate = new Date();

		//
		getYearMonth(datestr);
		this.id = this.code + "_" + this.reportDate;
	}

	private void getYearMonth(String datestr) {
		this.year = Integer.valueOf(datestr.substring(0, 4));
		int m = Integer.valueOf(datestr.substring(5, 7));
		if (m == 12) {
			this.quarter = 4;
		} else if (m == 9) {
			this.quarter = 3;
		} else if (m == 6) {
			this.quarter = 2;
		} else if (m == 3) {
			this.quarter = 1;
		} else {
			this.quarter = 0;
		}
		this.reportDate = Integer.valueOf(datestr.replace("-", ""));
	}
}
