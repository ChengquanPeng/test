package com.stable.web.controller;

import org.apache.commons.lang3.StringUtils;
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
	public ResponseEntity<JsonResult> list(String code, int asc, String conceptId, String conceptName, String baseLevel,
			int inMid, int midOk, int sortOk, int manualOk, String pe, String pettm, String pb, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {

			r.setResult(codePoolService.getListForWeb(code, conceptId, conceptName, asc,
					StringUtils.isNotBlank(baseLevel) ? Integer.valueOf(baseLevel) : 0, inMid, midOk, sortOk, manualOk,
					StringUtils.isNotBlank(pe) ? Double.valueOf(pe) : 0,
					StringUtils.isNotBlank(pettm) ? Double.valueOf(pettm) : 0,
					StringUtils.isNotBlank(pb) ? Double.valueOf(pb) : 0, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
