package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.job.EveryWeekMonJob;
import com.stable.vo.http.JsonResult;

@RequestMapping("/job")
@RestController
public class EveryWeekMonJobController {

	@Autowired
	private EveryWeekMonJob everyWeekMonJob;

	/**
	 * 根据 开始时间和结束时间 抓取回购信息
	 */
	@RequestMapping(value = "/everyWeek", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> daliycode() {
		JsonResult r = new JsonResult();
		try {
			everyWeekMonJob.execute(null);
			r.setResult(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
