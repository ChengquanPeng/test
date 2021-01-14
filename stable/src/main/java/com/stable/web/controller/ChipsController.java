package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.ChipsService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/chips")
@RestController
public class ChipsController {

	@Autowired
	private ChipsService chipsService;

	/**
	 * 根据code查询股东人数
	 */
	@RequestMapping(value = "/holdernum/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(chipsService.getHolderNumList45(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
