package com.stable.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.trace.HistTraceService;
import com.stable.utils.DateUtil;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RequestMapping("/retrace")
@RestController
@Log4j2
public class RetraceController {
	@Autowired
	private HistTraceService histTraceService;

	@RequestMapping(value = "/select", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sortv1(String startDate, String endDate, int days, double vb, String v) {
		JsonResult r = new JsonResult();
		try {
			log.info("startDate={},endDate={}", startDate, endDate);
			new Thread(new Runnable() {
				@Override
				public void run() {
					int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
					String s = startDate;
					if (StringUtils.isBlank(startDate)) {
						s = "20200101";
					}
					String e = endDate;
					if (StringUtils.isBlank(endDate)) {
						e = DateUtil.getTodayYYYYMMDD();
					}
					histTraceService.reallymodelForJob(v, s, e, days, vb, batch);
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

	@RequestMapping(value = "/middle", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> middle(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					histTraceService.middle(startDate, endDate);
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

	@RequestMapping(value = "/sortv2", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sortv2(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					histTraceService.sortv2(startDate, endDate);
				}
			}).start();

//			ThreadsUtil.sleepRandomSecBetween5And15();
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/sortv1", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sortv1(double min, double max, String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					histTraceService.sortv1(min, max, startDate, endDate);
				}
			}).start();

//			ThreadsUtil.sleepRandomSecBetween5And15();
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
