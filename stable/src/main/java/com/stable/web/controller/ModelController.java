package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.model.v1.UpLevel1Service;
import com.stable.vo.http.JsonResult;

@RequestMapping("/model")
@RestController
public class ModelController {

	@Autowired
	private UpLevel1Service upLevel1Service;

	/**
	 * 执行模型
	 */
	@RequestMapping(value = "/run", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> run(String date) {
		JsonResult r = new JsonResult();
		try {
			upLevel1Service.runJob(Integer.valueOf(date));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
