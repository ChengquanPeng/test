package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.CodePoolService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/codePool")
@RestController
public class CodePoolController {

	@Autowired
	private CodePoolService codePoolService;

	/**
	 * 根据code
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code, int orderBy, int asc, String conceptId, String conceptName,
			int baseLevel, int inMid, int midOk, int sortOk, int manualOk, double pe, double pettm, double pb,
			EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(codePoolService.getListForWeb(code, orderBy, conceptId, conceptName, asc, baseLevel, inMid,
					midOk, sortOk, manualOk, pe, pettm, pb, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
