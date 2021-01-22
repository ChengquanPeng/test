package com.stable.spider.eastmoney;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.stable.utils.HttpUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

//周六
@Component
@Log4j2
public class EastmoneyCompanySpider {
	private static final String URL_FORMAT = "http://f10.eastmoney.com/PC_HSF10/CompanySurvey/CompanySurveyAjax?code=%s&t=%s";

	/**
	 * 格式化成东方财富 API所需格式
	 */
	public static String formatCode2(String code) {
		// 5开头，沪市基金或权证 60开头上证
		if (code.startsWith("6")) {
			return String.format("SH%s", code);
		} else {
			return String.format("SZ%s", code);
		}
	}

	public void getCompanyInfo(StockBaseInfo b) {
		try {
			String code = b.getCode();
			JSONObject result = HttpUtil
					.doGet(String.format(URL_FORMAT, formatCode2(code), System.currentTimeMillis()));
			JSONObject data = (JSONObject) result.get("jbzl");
//			System.err.println(data.get("cym"));
//			System.err.println(data.get("gswz"));
			b.setOldName((String) data.get("cym"));
			b.setWebSite((String) data.get("gswz"));
		} catch (Exception e) {
			log.error("code={} 获取曾用名和网站出错", b.getCode());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		EastmoneyCompanySpider a = new EastmoneyCompanySpider();
		StockBaseInfo b = new StockBaseInfo();
		b.setCode("000001");
		a.getCompanyInfo(b);
	}
}
