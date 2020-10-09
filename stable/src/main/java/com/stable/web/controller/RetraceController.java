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
			int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
			String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
			if (StringUtils.isBlank(startDate)) {
				startDate = "20200101";
			}
			if (StringUtils.isBlank(endDate)) {
				endDate = DateUtil.getTodayYYYYMMDD();
			}
			log.info("startDate={},endDate={}", startDate, endDate);
			histTraceService.reallymodelForJob(v, startDate, endDate, days, vb, sysstart, batch);
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
}
