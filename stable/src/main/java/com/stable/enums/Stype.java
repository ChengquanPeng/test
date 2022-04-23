package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Stype {
	webQuery(1, "系统查询"), sysChance(2, "系统推荐机会");

	private int code;
	private String desc;

	public static String getCodeName(int code) {
		for (Stype c : Stype.values()) {
			if (c.getCode() == code) {
				return c.desc;
			}
		}
		return code + "未知";
	}
}
