package com.stable.vo.retrace;

import lombok.Data;

@Data
public class TraceSortv2Vo {
	private String code;
	private int date;
	private double buyPrice;
	private double sellPrice;
	private double maxPrice;
	private double minPrice;
}