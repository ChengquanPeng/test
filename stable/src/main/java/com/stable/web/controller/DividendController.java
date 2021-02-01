package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.BonusService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/dividend/hist")
@RestController
public class DividendController {

	@Autowired
	private BonusService dividendService;

	/**
	 * 获取分红信息
	 */
	@RequestMapping(value = "/query", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> query(String code, String zsg, String proc, String queryYear,
			EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			r.setResult(dividendService.getListByCodeForWebPage(code, zsg, proc, queryYear, page));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 获取分红信息
	 */
	@RequestMapping(value = "/list/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(@PathVariable(value = "code") String code, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			r.setResult(dividendService.getListByCodeForWebPage(code, null, null, null, page));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 获取分红信息
	 */
	@RequestMapping(value = "/listall", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> listall(EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			r.setResult(dividendService.getListByCodeForWebPage(null, null, null, null, page));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
