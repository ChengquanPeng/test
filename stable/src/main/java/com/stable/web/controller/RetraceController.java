package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.model.up.retrace.shorts.ShFiveDaysDownService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/retrace")
@RestController
public class RetraceController {

	@Autowired
	private ShFiveDaysDownService shFiveDaysDownService;

	/**
	 * 根据 开始时间和结束时间 抓取回购信息
	 */
	@RequestMapping(value = "/sh/shFiveDaysDown", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetch(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			shFiveDaysDownService.manualHistory();
			r.setResult(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
