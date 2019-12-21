package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.RunLogService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/runlog")
@RestController
public class RunlogController {

	@Autowired
	private RunLogService runLogService;

	/**
	 * 根据code查询财务信息
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(Integer btype, Integer date, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			Integer p1 = null;
			if (btype!=null && btype > 0) {
				p1 = Integer.valueOf(btype);
			}
			Integer p2 = null;
			if (date!=null && date > 0) {
				p2 = Integer.valueOf(date);
			}
			r.setResult(runLogService.queryRunlogs(p1, p2, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
