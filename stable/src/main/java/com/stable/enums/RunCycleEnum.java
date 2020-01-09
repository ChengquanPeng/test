package com.stable.enums;

public enum RunCycleEnum {
	MANUAL(4, "手动 MANUAL"), 
	MONTH(1, "每月 MONTH"), 
	WEEK(2, "每周 WEEK"), 
	DAY(3, "每天 DAY");

	public int code;
	public String name;

	private RunCycleEnum(int c, String n) {
		this.code = c;
		this.name = n;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public static RunCycleEnum getRunCycleEnum(int code) {
		for (RunCycleEnum rb : RunCycleEnum.values()) {
			if (rb.getCode() == code) {
				return rb;
			}
		}
		return null;
	}
}
