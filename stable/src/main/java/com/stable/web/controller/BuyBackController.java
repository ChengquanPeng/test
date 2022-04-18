package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.BuyBackService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/buyback")
@RestController
public class BuyBackController {

	@Autowired
	private BuyBackService buyBackService;

	/**
	 * 根据code查询财务信息
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code, int dtype, int asc, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(buyBackService.getListByCodeForWebPage(code, dtype, asc, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/list/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> listcode(@PathVariable(value = "code") String code, int dtype, int asc,
			EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(buyBackService.getListByCodeForWebPage(code, dtype, asc, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
