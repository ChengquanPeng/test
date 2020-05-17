package com.stable.util;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class HtmlunitSpider {

	public String getFromUrl(String url) {
		WebClient webclient = WebClientPoolFactory.getWebClient();
		try {
			HtmlPage page = webclient.getPage(url);
			return page.getWebResponse().getContentAsString();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return "";
		} finally {
			WebClientPoolFactory.returnBean(webclient);
		}
	}

	public HtmlPage getHtmlPageFromUrl(String url, Map<String, String> header) {
		WebClient webclient = WebClientPoolFactory.getWebClient();
		try {
			if (header != null) {
				header.keySet().forEach(key -> {
					webclient.addRequestHeader(key, header.get(key));
				});
			}
			return webclient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return null;
		} finally {
			WebClientPoolFactory.returnBean(webclient);
		}
	}

	public HtmlPage getHtmlPageFromUrl(String url) {
		WebClient webclient = WebClientPoolFactory.getWebClient();
		try {
			return webclient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return null;
		} finally {
			WebClientPoolFactory.returnBean(webclient);
		}
	}
}
