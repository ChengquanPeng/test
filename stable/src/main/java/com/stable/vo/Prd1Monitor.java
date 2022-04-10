package com.stable.vo;

import com.stable.vo.bus.OnlineTesting;
import com.stable.vo.bus.Prd1;

import lombok.Data;

@Data
public class Prd1Monitor {
	private String code;
	private Prd1 buy;
	private OnlineTesting onlineTesting;
}
