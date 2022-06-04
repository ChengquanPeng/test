package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MonitorType {
	NO(0, "无"), MANUAL(3, "人工"), Reduce(6, "底部小票-减持"), PreZengFa(7, "底部大票-增发已核准"), ZengFaAuto(8, "底部小票-增发已完成-3y+未涨"),
	ZengFaAuto2(9, "底部小票-增发已完成-2y"), SORT1(11, "短线2:确定极速拉升带小平台新高？"), DZJY(41, "底部小票-大宗");

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
