package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.job.EveryMonthJob;
import com.stable.job.EveryWeekMonJob;
import com.stable.job.EveryWorkingDayJob;
import com.stable.job.EveryWorkingDayMorningJob;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/job")
@RestController
public class JobController {

	@Autowired
	private EveryWeekMonJob everyWeekMonJob;
	@Autowired
	private EveryMonthJob everyMonthJob;
	@Autowired
	private EveryWorkingDayJob everyWorkingDayJob;
	@Autowired
	private EveryWorkingDayMorningJob everyWorkingDayMorningJob;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;

	/**
	 * 每天早上任务
	 */
	@RequestMapping(value = "/everyWorkingDayMorning", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> everyWorkingDayMorningJob() {
		JsonResult r = new JsonResult();
		try {
			everyWorkingDayMorningJob.execute(null);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 每天晚上任务
	 */
	@RequestMapping(value = "/everyWorkingDay", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> everyWorkingDayJob() {
		JsonResult r = new JsonResult();
		try {
			everyWorkingDayJob.execute(null);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 每周1任务
	 */
	@RequestMapping(value = "/everyWeekMon", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> everyWeekMonJob() {
		JsonResult r = new JsonResult();
		try {
			everyWeekMonJob.execute(null);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 每月任务
	 */
	@RequestMapping(value = "/everyMonth", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> everyMonthJob() {
		JsonResult r = new JsonResult();
		try {
			everyMonthJob.execute(null);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 手动同步交易日历
	 */
	@RequestMapping(value = "/tradecal", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> tradecal(int startdate, int enddate) {
		JsonResult r = new JsonResult();
		try {
			tradeCalService.josSynTradeCal2(startdate + "", enddate + "");
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 手动同步交易日历
	 */
	@RequestMapping(value = "/stocklist", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> stocklist() {
		JsonResult r = new JsonResult();
		try {
			stockBasicService.jobSynStockList();
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
