package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TradeType {
	// 1:waiting buy(等待成交),2 bought(已买),3 sold(已卖)
	WAITING(1, "等待成交"), BOUGHT(2, "已买"), SOLD(3, "已卖");

	private int code;
	private String desc;
}
