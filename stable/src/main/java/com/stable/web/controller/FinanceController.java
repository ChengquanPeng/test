package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.FinanceService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/finance/hist")
@RestController
public class FinanceController {

	@Autowired
	private FinanceService financeService;

	/**
	 * 根据code 抓取财务信息
	 */
	@RequestMapping(value = "/fetch/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> daliycode(@PathVariable(value = "code") String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(financeService.spiderFinaceHistoryInfoFromStart(code));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 获取所有财务信息
	 */
	@RequestMapping(value = "/fetchall", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> daliyall() {
		JsonResult r = new JsonResult();
		try {
			financeService.jobSpiderFinaceHistoryInfo();
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
	@RequestMapping(value = "/list/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> listcode(@PathVariable(value = "code") String code, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(financeService.getFinaceReports(code, page));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
