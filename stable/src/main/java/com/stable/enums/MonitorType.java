package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MonitorType {
	SUSPECT_BIG_BOSS(1, "疑似大牛"), MID(2, "疑似白马"), MANUAL(3, "人工"), SORT(4, "短线"), ZengFa(5, "增发"),
	ZengFaAuto(6, "增发-系统自动"), HolderNum(7, "股东人数"), Buy_Low_Vol(8, "买点-地量"), NO(0, "无"), SMALL_AND_BEAUTIFUL(9, "小而美");

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
