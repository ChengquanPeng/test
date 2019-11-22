package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.BuyBackService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/buyback")
@RestController
public class BuyBackController {

	@Autowired
	private BuyBackService buyBackService;

	/**
	 * 根据 开始时间和结束时间 抓取回购信息
	 */
	@RequestMapping(value = "/fetch", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> daliycode(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			if (startDate.length() != 8 || endDate.length() != 8) {
				r.setStatus(JsonResult.FAIL);
				return ResponseEntity.ok(r);
			}
			Integer.parseInt(startDate);
			Integer.parseInt(endDate);
			buyBackService.spiderBuyBackHistoryInfo(startDate, endDate);
			r.setResult(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code查询财务信息
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(int dtype,int asc) {
		JsonResult r = new JsonResult();
		try {
			 r.setResult(buyBackService.getBuyBackInfo(null, dtype,asc));
			 r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
	
	@RequestMapping(value = "/list/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> listcode(@PathVariable(value = "code") String code,int dtype,int asc) {
		JsonResult r = new JsonResult();
		try {
			 r.setResult(buyBackService.getBuyBackInfo(code, dtype,asc));
			 r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
