package com.stable.web.controller;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.PlateService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.http.resp.PlateResp;

@RequestMapping("/plate")
@RestController
public class PlateController {

	@Autowired
	private PlateService plateService;

	/**
	 * 板块分析
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String aliasCode, String codes, String sort) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(plateService.plateAnalyse(aliasCode, codes, Integer.valueOf(sort)));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 板块K线形态
	 */
	@RequestMapping(value = "/klinelist", method = RequestMethod.GET)
	public void klinelist(String code, HttpServletResponse response) {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html;charset=utf-8");
		try {
			PrintWriter w = response.getWriter();
			List<PlateResp> list = plateService.klinelist();
			if (list != null && list.size() > 0) {
				w.write("攻击形态板块排序<br/><table><tr><td>code</td><td>name</td><td>排名</td><td>攻击数量/总数量</td><tr/>");
				for (PlateResp row : list) {
					w.write("<tr><td>" + row.getCode() + "</td><td>" + row.getCodeName() + "</td><td>" + row.getT4()
							+ "</td><td>" + row.getRanking1() + "/" + row.getRanking2() + "</td><tr/>");
				}
				w.write("</table>");
			} else {
				w.write(code + "无数据");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
