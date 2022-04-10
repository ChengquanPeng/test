package com.stable.vo;

import com.stable.vo.bus.OnlineTesting;

import lombok.Data;

@Data
public class Prd1Monitor {
	private String code;
	private boolean buy;
	private OnlineTesting onlineTesting;
}
