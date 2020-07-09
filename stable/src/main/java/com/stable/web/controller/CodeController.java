package com.stable.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.stable.service.realtime.MonitoringService;
import com.stable.vo.http.resp.ReportVo;
import com.stable.vo.http.resp.ViewVo;

@Controller
public class CodeController {

	@Autowired
	private MonitoringService monitoringService;

	/**
	 * 根据 开始时间和结束时间 抓取回购信息
	 */
	@RequestMapping(value = "/code/{code}", method = RequestMethod.GET)
	public String detail(@PathVariable(value = "code") String code, Model model) {
		try {
			model.addAttribute("code", code);
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
