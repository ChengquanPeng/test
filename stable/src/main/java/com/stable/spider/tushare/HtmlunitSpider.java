package com.stable.spider.tushare;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.utils.WebClientPoolFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlunitSpider {

	public static void main(String[] args) {
		WebClient webclient = WebClientPoolFactory.getWebClient();
		try {
			String url = "http://basic.10jqka.com.cn/600000/finance.html";
			HtmlPage page = webclient.getPage(url);
			String text = page.getElementById("main").getTextContent();
			JSONObject jo = JSONObject.parseObject(text);
			JSONArray report = jo.getJSONArray("report");
			int size = report.size();
			for (int i = 0; i < size; i++) {
				log.info("index " + i + ":" + report.get(i));
			}
			log.info("===========");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			WebClientPoolFactory.returnBean(webclient);
		}
	}
}
