package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.TradeCalService;
import com.stable.service.model.CodeModelService;
import com.stable.utils.DateUtil;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/model")
@RestController
public class CodeModelController {

	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private TradeCalService tradeCalService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> codemodellist(String code, int orderBy, int asc, String conceptId,
			String conceptName, String zfStatus, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(codeModelService.getListForWeb(code, orderBy, conceptId, conceptName, asc, page, zfStatus));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 执行模型（基本面)
	 */
	@RequestMapping(value = "/coderun", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> coderun() {
		JsonResult r = new JsonResult();
		try {
			int date = DateUtil.getTodayIntYYYYMMDD();
			date = tradeCalService.getPretradeDate(date);
			// codeModelService.reset();
			codeModelService.runJobv2(false, date);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
