package com.stable.web.controller;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.StockBasicService;
import com.stable.service.trace.HistTraceService;
import com.stable.utils.DateUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RequestMapping("/retrace")
@RestController
@Log4j2
public class RetraceController {
	@Autowired
	private HistTraceService histTraceService;
	@Autowired
	private StockBasicService stockBasicService;

	@RequestMapping(value = "/select", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sortv1(String startDate, String endDate, int days, int vb, String v) {
		JsonResult r = new JsonResult();
		try {
			int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
			String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
			if (StringUtils.isBlank(startDate)) {
				startDate = "20200101";
			}
			if (StringUtils.isBlank(endDate)) {
				endDate = DateUtil.getTodayYYYYMMDD();
			}
			log.info("startDate={},endDate={}", startDate, endDate);

			if ("v2".equals(v)) {
				histTraceService.v2Really(startDate, endDate, codelist, days, 1, vb, sysstart, batch);
			} else if ("v3".equals(v)) {
				histTraceService.v3Really(startDate, endDate, codelist, days, 1, vb, sysstart, batch);
			}
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

			ThreadsUtil.sleepRandomSecBetween5And15();

			new Thread(new Runnable() {
				@Override
				public void run() {
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
