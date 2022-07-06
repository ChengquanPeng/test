package com.stable.web.controller;

import javax.servlet.http.HttpServletResponse;

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
	public ResponseEntity<JsonResult> list(String aliasCode, String codes, String sort) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(plateService.plateAnalyse(aliasCode, codes, Integer.valueOf(sort)));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 板块K线形态
	 */
	@RequestMapping(value = "/klinelist", method = RequestMethod.GET)
	public void klinelist(String code, HttpServletResponse response) {
		try {
			plateService.getPlateStat();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
