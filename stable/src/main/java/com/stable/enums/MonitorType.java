package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MonitorType {
	NO(0, "无"), MANUAL(3, "人工"), ZengFaAuto(6, "增发-系统自动"), SORT1(11, "短线1");

	private int code;
	private String desc;

	public static String getCodeName(int code) {
		for (MonitorType c : MonitorType.values()) {
			if (c.getCode() == code) {
				return c.desc;
			}
		}
		return code + NO.desc;
	}
}
