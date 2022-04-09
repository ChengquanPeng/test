package com.stable.spider.tick;

public class TencentTick {
	public static String getCode(String code) {
		if (code.startsWith("6")) {
			return "sh" + code;
		}
		return "sz" + code;
	}
}
