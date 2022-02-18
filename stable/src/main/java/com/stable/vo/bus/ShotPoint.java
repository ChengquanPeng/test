package com.stable.vo.bus;

import lombok.Data;

@Data
public class ShotPoint {

	private int v1 = 0;

	public boolean getResult() {
		if (v1 == 1) {
			return true;
		}
		return false;
	}

	public String getMsg() {
		if (v1 == 1) {
			return "中阳带量";
		}
		return "";
	}
}
