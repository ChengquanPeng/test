package com.stable.vo.bus;

import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "buy_trace")
public class BuyTrace extends EsBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6862288561607105101L;
	private String id;
	private String code;
	private int buyDate;
	private double buyPrice;
	private int status;// 0:waiting buy(等待成交),bought(已买),sold(已卖)
	private int soldDate;
	private double profit;
}
