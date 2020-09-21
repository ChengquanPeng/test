package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.trace.HistTraceService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/retrace")
@RestController
public class RetraceController {
	@Autowired
	private HistTraceService histTraceService;

	@RequestMapping(value = "/sortv1", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sortv1(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			histTraceService.sortv1(startDate, endDate);
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/sortv2", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sortv2(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			new Thread(new Runnable() {

				@Override
				public void run() {
					histTraceService.sortv2(startDate, endDate);
					histTraceService.sortv3(startDate, endDate);
				}
			}).start();
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
