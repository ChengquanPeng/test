package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ZfStatus {
	NO(0, "无"), ING(1, "正在增发"), DONE(2, "完成增发"), ZUOFEI(3, "增发作废"), ZF_ZJHHZ(18, "证监会核准");

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
