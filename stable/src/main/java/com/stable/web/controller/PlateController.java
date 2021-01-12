package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.PlateService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/plate")
@RestController
public class PlateController {

	@Autowired
	private PlateService plateService;

	/**
	 * 板块分析
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String aliasCode, String codes) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(plateService.plateAnalyse(aliasCode, codes));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
