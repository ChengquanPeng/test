package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.realtime.MonitoringService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/realtime")
@RestController
public class RealTimeController {

	@Autowired
	private MonitoringService monitoringService;


	/**
	 * 交易详情
	 */
	@RequestMapping(value = "/detail", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> detail(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(monitoringService.todayBillingDetailReport(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 终止线程
	 */
	@RequestMapping(value = "/stop", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> stop(String code) {
		JsonResult r = new JsonResult();
		try {
			monitoringService.stopThread(code);
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 实时买入-人工
	 */
	@RequestMapping(value = "/buy", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> buy(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(monitoringService.buy(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 实时买出-人工
	 */
	@RequestMapping(value = "/sell", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sell(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(monitoringService.sell(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
