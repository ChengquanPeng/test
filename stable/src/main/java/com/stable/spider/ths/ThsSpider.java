package com.stable.spider.ths;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.constant.Constant;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.TheadUtil;
import com.stable.vo.bus.FinanceBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ThsSpider {

	@Autowired
	private HtmlunitSpider htmlunitSpider;

	private static final String FIN_RPT_URL = "http://basic.10jqka.com.cn/%s/finance.html";

	public List<FinanceBaseInfo> getBaseFinance(String code) {
		String text;
		try {
			HtmlPage page = htmlunitSpider.getHtmlPageFromUrl(String.format(FIN_RPT_URL, code));
			text = page.getElementById("main").getTextContent();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			TheadUtil.thsSleepRandom();
		}
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
			try {
				finbase.setValue(code, j, list);
				log.debug(finbase.toString());
				res.add(finbase);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(list.get(0).getString(j));
				log.error(finbase.toString());
				return null;
			}
		}
		log.debug("HtmlunitSpider for code={}", code);
		return res;
	}
}
