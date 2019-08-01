package com.stable.vo.bus;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.alibaba.fastjson.JSONArray;
import com.stable.utils.CurrencyUitl;

import lombok.Data;

@Data
@Document(indexName = "finance_base_info")
public class FinanceBaseInfo {
	@Id
	private String code;
	//时间
	private String date;
//  基本每股收益
	private String earningsPerShare;
//	净利润(元)
	private Long retainedProfits;
//	净利润同比增长率
	private String retainedProfitsTbRate;
//	扣非净利润(元)
	private Long retainedProfitsReal;
//	扣非净利润同比增长率
	private String retainedProfitsRealTbRate;
//	营业总收入(元)
	private Long income;
//	营业总收入同比增长率
	private String incomeTbRate;
//	每股净资产
	private String assetPerShare;
//	净资产收益率
	private String assetEarningseRate;
//	净资产收益率-摊薄
	private String assetEarningseRateDilute;
//	资产负债比率
	private String assetDebtRate;
//	每股资本公积金(元)
	private String fundPerShare;
//	每股未分配利润(元)
	private String unpaidProfitsPerShare;
//	每股经营现金流
	private String cashPerShare;
//	销售净利率 
	private String profitOnSales;

	public void setValue(String code, int index, List<JSONArray> list) {
		this.code = code;
		int indexlist = 0;
		this.date = list.get(indexlist++).getString(index);
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
	}
}
