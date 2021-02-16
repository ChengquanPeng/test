package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CodeModeType {
	SUSPECT_BIG_BOSS(1, "疑似大牛"), MID(2, "疑似白马"), MANUAL(3, "人工"), SORT(4, "短线"), ZengFa(5, "增发"),
	ZengFaAuto(6, "增发-系统自动"), NO(0, "无");

	private int code;
	private String desc;

	public static String getCodeName(int code) {
		for (CodeModeType c : CodeModeType.values()) {
			if (c.getCode() == code) {
				return c.desc;
			}
		}
		return code + NO.desc;
	}
}
