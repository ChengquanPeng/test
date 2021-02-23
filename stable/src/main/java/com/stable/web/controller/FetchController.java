package com.stable.web.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.spider.ths.ThsSpider;
import com.stable.vo.bus.Concept;
import com.stable.vo.http.JsonResult;

@RequestMapping("/fetch/m")
@RestController
public class FetchController {

	@Autowired
	private ThsSpider thsSpider;

	@RequestMapping(value = "/thsConceptDaily", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> query(String code, int date) {
		JsonResult r = new JsonResult();
		try {
			Concept cp = null;
			Map<String, Concept> m = thsSpider.getAllAliasCode();
			for (String key : m.keySet()) {
				if (code.equals(m.get(key).getAliasCode2())) {
					cp = m.get(key);
				}
			}
			r.setStatus(JsonResult.OK);
			r.setResult(thsSpider.getConceptDaily(cp, cp.getHref(), date));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
