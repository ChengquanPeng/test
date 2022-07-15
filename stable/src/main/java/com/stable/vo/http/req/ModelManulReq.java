package com.stable.vo.http.req;

import lombok.Data;

@Data
public class ModelManulReq {
	String code;
	int pls;
	int timemonth;
	private String buyRea; // 买入理由
}
