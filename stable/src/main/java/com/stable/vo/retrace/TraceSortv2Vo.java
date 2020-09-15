package com.stable.vo.retrace;

import lombok.Data;

@Data
public class TraceSortv2Vo {
	private String code;
	private int date;
	private double buyPrice;
	private double maxPrice;
	private double minPrice;
	private double maxProfit;// 最高盈利
	private double maxLoss;// 最低盈利
}