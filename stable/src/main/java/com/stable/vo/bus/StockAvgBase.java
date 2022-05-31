package com.stable.vo.bus;

import lombok.Data;

@Data
public class StockAvgBase {

	private String code;
	private int date;

	private double avgPriceIndex5;
	private double avgPriceIndex10;
	private double avgPriceIndex20;
	private double avgPriceIndex30;
	private double avgPriceIndex60;
	private double avgPriceIndex120;
	private double avgPriceIndex250;
	private double closePrice;
	private double updown;
}
