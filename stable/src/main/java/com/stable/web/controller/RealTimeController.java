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
	 * 实时买入，并终止线程
	 */
	@RequestMapping(value = "/buy", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(monitoringService.buyAndStopThread(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
