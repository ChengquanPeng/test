package com.stable.spider.ths;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.constant.Constant;
import com.stable.utils.HtmlunitSpider;
import com.stable.vo.bus.FinanceBaseInfo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ThsSpider {

	@Autowired
	private HtmlunitSpider htmlunitSpider;

	private static final String FIN_RPT_URL = "http://basic.10jqka.com.cn/%s/finance.html";

	public List<FinanceBaseInfo> getBaseFinance(String code) {
		HtmlPage page = htmlunitSpider.getHtmlPageFromUrl(String.format(FIN_RPT_URL, code));
		String text = page.getElementById("main").getTextContent();
		text = text.replace(Constant.FALSE, Constant.EMPTY_STRING2);
		JSONObject jo = JSONObject.parseObject(text);
		JSONArray report = jo.getJSONArray("report");

		List<JSONArray> list = new ArrayList<JSONArray>();
		for (int j = 0; j < report.size(); j++) {
			list.add(report.getJSONArray(j));
		}
		List<FinanceBaseInfo> res = new LinkedList<FinanceBaseInfo>();
		for (int j = 0; j < list.get(0).size(); j++) {
			FinanceBaseInfo finbase = new FinanceBaseInfo();
			finbase.setValue(code, j, list);
			log.debug(finbase.toString());
			res.add(finbase);
		}
		log.debug("HtmlunitSpider for code={}", code);
		return res;
	}

	// @PostConstruct
	private void test() {
		getBaseFinance("600000");
	}
}
