package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ZfStatus {
	NO(0, "无"), UP(1, "增长"), C20(2, "年超20%"), BOTH(4, "增长&超20%");

	private int code;
	private String desc;

	public static String getCodeName(int code) {
		for (ZfStatus c : ZfStatus.values()) {
			if (c.getCode() == code) {
				return c.desc;
			}
		}
		return code + NO.desc;
	}
}
