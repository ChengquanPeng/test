package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BuyModelType {
	B1(1, "B1人工买入"), B2(2, "B2机器实时买入");

	private int code;
	private String desc;
}
