package com.stable.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class CodeController {

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
}
