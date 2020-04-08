package com.stable.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.TickDataService;
import com.stable.utils.DateUtil;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/tickdata")
@RestController
public class TickDataController {

	@Autowired
	private TickDataService tickDataService;

	/**
	 * 根据code重新获取历史记录（前复权）
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code, String date, String program, EsQueryPageReq queryPage) {
		JsonResult r = new JsonResult();
		try {
			if (StringUtils.isBlank(code) && StringUtils.isBlank(date)) {
				date = DateUtil.getTodayYYYYMMDD();
			}
			r.setResult(tickDataService.listForWebPage(code, date, program, queryPage));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code重新获取历史记录（前复权） all{1,0}
	 */
	@RequestMapping(value = "/fetch", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetch(String code, String date, String all, String html, String startDate) {
		JsonResult r = new JsonResult();
		try {
			tickDataService.fetch(code, date, all, "1".equals(html), startDate);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 在线修改tick data 开始时间
	 */
	@RequestMapping(value = "/updateStartDate", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetch(String startDate) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(tickDataService.updateStartDateOnline(startDate));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 重置开始时间内的未获取的tick status
	 */
	@RequestMapping(value = "/reTickDataStatus", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetch() {
		JsonResult r = new JsonResult();
		try {
			tickDataService.resetTickDataStatus();
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
