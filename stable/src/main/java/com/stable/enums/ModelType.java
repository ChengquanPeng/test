package com.stable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ModelType {
	V1(1, "V1横盘突破"), V2(2, "V2浪均线突破"), IMAGE(3, "图片对比模型"), V2_PRE(4, "V2浪均线突破PRE");

	private int code;
	private String desc;
}
