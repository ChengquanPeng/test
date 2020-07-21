package com.stable.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.service.realtime.MonitoringService;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.http.resp.ReportVo;
import com.stable.vo.http.resp.ViewVo;
import com.stable.vo.spi.req.EsQueryPageReq;

@Controller
public class CodeController {

	@Autowired
	private MonitoringService monitoringService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;
	private EsQueryPageReq querypage = new EsQueryPageReq(10);

	/**
	 * 个股当前状态
	 */
	@RequestMapping(value = "/code/{code}", method = RequestMethod.GET)
	public String detail(@PathVariable(value = "code") String code, Model model) {
		try {
			model.addAttribute("codedetail", codeModelService.getLastOneByCode(code));
			model.addAttribute("code", code);
			model.addAttribute("codeName", stockBasicService.getCodeName(code));
			model.addAttribute("histList", codeModelService.getListByCode(code, querypage));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

	/**
	 * 个股历史状态
	 */
	@RequestMapping(value = "/codehist/{id}", method = RequestMethod.GET)
	public String id(@PathVariable(value = "id") String id, Model model) {
		try {
			CodeBaseModelHist ch = codeModelService.getHistOneById(id);
			String code = ch.getCode();
			model.addAttribute("code", code);
			model.addAttribute("codeName", stockBasicService.getCodeName(code));
			model.addAttribute("codedetail", ch);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

	/**
	 * 测试页面列表
	 */
	@RequestMapping(value = "/realtime/view", method = RequestMethod.GET)
	public String view(String all, String type, String buyDate, Model model) {
		try {
			ReportVo pv = monitoringService.getVeiw(all, type, buyDate);
			List<ViewVo> l = pv.getList();
			model.addAttribute("vvs", l);
			model.addAttribute("stat", pv);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "realtimelist";
	}
}
