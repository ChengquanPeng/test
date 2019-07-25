package com.stable.vo.http;

import lombok.Data;

@Data
public class JsonResult {

	private String status = null; // 返回的数据
	private Object result = null; // 返回的状态

	public JsonResult status(String status) {
		this.status = status;
		return this;
	}
}
