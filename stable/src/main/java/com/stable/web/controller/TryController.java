package com.stable.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.model.WebModelService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.http.req.ModelReq;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/try")
@RestController
public class TryController {
	private static final String T1 = "试用页面不支持个股查询";
	@Autowired
	private WebModelService modelWebService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> codemodellist(ModelReq mr, EsQueryPageReq querypage, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			mr.setTrymsg(true);
			List<CodeBaseModelResp> l = null;
			if (StringUtils.isNotBlank(mr.getCode())) {
				l = new ArrayList<CodeBaseModelResp>();
				CodeBaseModelResp cr = new CodeBaseModelResp();
				cr.setId("00000");
				cr.setCode("00000");
				cr.setCodeName("XX股份");
				cr.setZfjjInfo(T1);
				cr.setTagInfo(T1);
				cr.setBuyRea(T1);
				cr.setBaseInfo(T1);
				l.add(cr);
			} else {
				l = modelWebService.getListForWeb(mr, querypage, 999999);
				if (l != null) {
					for (CodeBaseModelResp cr : l) {
						cr.setBankuai("");
						cr.setId("00000");
						cr.setCode("xx" + cr.getCode().charAt(2) + "xx" + cr.getCode().charAt(5));
						try {
							cr.setCodeName("XXX" + cr.getCodeName().charAt(cr.getCodeName().length() - 1));
						} catch (Exception e) {
							cr.setCodeName("XX");
						}
					}
				}
			}
			r.setResult(l);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

}
