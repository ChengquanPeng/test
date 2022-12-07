package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.stable.service.datamv.DataMovingNewer;

//@RestController
//@RequestMapping("/datamv")
public class DataMovingController {

	@Autowired
	private DataMovingNewer dataMovingNewer;

	@RequestMapping(value = "/get")
	@ResponseBody
	public String get(String tableName, int pageNum, int pageSize) {
		return JSON.toJSONString(dataMovingNewer.fetchData(tableName, pageNum, pageSize));
	}

	@RequestMapping(value = "/move")
	@ResponseBody
	public String move(String tableName, int pageSize) {
		dataMovingNewer.executeMoveByTable(tableName, pageSize);
		return "OK-" + tableName;
	}
}
