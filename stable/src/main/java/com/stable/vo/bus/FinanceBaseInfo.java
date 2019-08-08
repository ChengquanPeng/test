package com.stable.vo.bus;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import com.alibaba.fastjson.JSONArray;
import com.stable.utils.CurrencyUitl;

import lombok.Data;

@Data
@Document(indexName = "finance_base_info")
public class FinanceBaseInfo {
	@Id
	private String id;
	@Field
	private String code;
	// 时间
	@Field
	private int reportDate;
	@Field
	private int year;
	@Field
	private int quarter;
	@Field
//  基本每股收益
	private String earningsPerShare;
	@Field
//	净利润(元)
	private Long retainedProfits;
	@Field
//	净利润同比增长率
	private String retainedProfitsTbRate;
	@Field
//	扣非净利润(元)
	private Long retainedProfitsReal;
	@Field
//	扣非净利润同比增长率
	private String retainedProfitsRealTbRate;
	@Field
//	营业总收入(元)
	private Long income;
//	营业总收入同比增长率
	private String incomeTbRate;
	@Field
//	每股净资产
	private String assetPerShare;
	@Field
//	净资产收益率
	private String assetEarningseRate;
	@Field
//	净资产收益率-摊薄
	private String assetEarningseRateDilute;
	@Field
//	资产负债比率
	private String assetDebtRate;
	@Field
//	每股资本公积金(元)
	private String fundPerShare;
	@Field
//	每股未分配利润(元)
	private String unpaidProfitsPerShare;
	@Field
//	每股经营现金流
	private String cashPerShare;
	@Field
//	销售净利率 
	private String profitOnSales;
	@Field
	private Date updateDate;

	public void setValue(String code, int index, List<JSONArray> list) {
		this.code = code;
		int indexlist = 0;
		String datestr = list.get(indexlist++).getString(index);
		getYearMonth(datestr);
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
