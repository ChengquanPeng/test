package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MonitoringType {
	BUY(1, "买入"), SELL(2, "卖出"), ALL(3, "卖出");

	private int code;
	private String desc;
}
