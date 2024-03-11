package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MonitorType {
	NO(0, "无"), MANUAL(3, "人工"), Reduce(6, "底部小票减持"), PreZengFa(7, "底部大票定增"), ZengFaAuto(8, "底部小票定增"),
	DZJY(41, "底部小票大宗"), SmallLow(40, "底部优质小票");
	private int code;
	private String desc;

	public static String getCodeName(int code) {// 底部大票-增发已核准
		for (MonitorType c : MonitorType.values()) {
			if (c.getCode() == code) {
				return c.desc;
			}
		}
		return code + NO.desc;
	}

	public static String getCode(int code) {// PreZengFa
		for (MonitorType c : MonitorType.values()) {
			if (c.getCode() == code) {
				return c.toString();
			}
		}
		return code + NO.desc;
	}

	public static void main(String[] args) {
		System.err.println(MonitorType.getCodeName(7));
		System.err.println(MonitorType.getCode(7));
	}
}
