package com.stable.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDailyDao;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.vo.bus.Concept;
import com.stable.vo.http.JsonResult;
import com.stable.vo.http.resp.CodeConceptAddReq;
import com.stable.vo.http.resp.ConceptAddReq;
import com.stable.vo.http.resp.ConceptDaliyAddReq;

@RequestMapping("/concept")
@RestController
public class ConceptController {

	@Autowired
	private EsConceptDao esConceptDao;
	@Autowired
	private EsCodeConceptDao esCodeConceptDao;
	@Autowired
	private EsConceptDailyDao esConceptDailyDao;

	@PostMapping(value = "/allConcepts")
	public ResponseEntity<JsonResult> allConcepts() {
		JsonResult r = new JsonResult();
		try {
			Map<String, Concept> m = new HashMap<String, Concept>();
			esConceptDao.findAll().forEach(x -> {
				if (StringUtils.isNotBlank(x.getAliasCode2()) && !"null".equals(x.getAliasCode2())) {
					m.put(x.getCode(), x);
				}
			});
			r.setResult(m);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@PostMapping(value = "/addConcept")
	public ResponseEntity<JsonResult> addConcept(@RequestBody ConceptAddReq req) {
		JsonResult r = new JsonResult();
		try {
			if (req.getList() != null && req.getList().size() > 0) {
				esConceptDao.saveAll(req.getList());
			}
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@PostMapping(value = "/addCodeConcept")
	public ResponseEntity<JsonResult> addCodeConcept(@RequestBody CodeConceptAddReq req) {
		JsonResult r = new JsonResult();
		try {
			if (req.getList() != null && req.getList().size() > 0) {
				esCodeConceptDao.saveAll(req.getList());
			}
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@PostMapping(value = "/addConceptDaily")
	public ResponseEntity<JsonResult> addConceptDaily(@RequestBody ConceptDaliyAddReq req) {
		JsonResult r = new JsonResult();
		try {
			if (req.getList() != null && req.getList().size() > 0) {
				esConceptDailyDao.saveAll(req.getList());
			}
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
