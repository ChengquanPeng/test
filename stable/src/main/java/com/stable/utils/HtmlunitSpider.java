package com.stable.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class HtmlunitSpider {

	private WebClient create() {
		WebClient webClient = new WebClient();
		webClient = new WebClient(BrowserVersion.CHROME);// 新建一个模拟谷歌Chrome浏览器的浏览器客户端对象
		webClient.getOptions().setThrowExceptionOnScriptError(false);// 当JS执行出错的时候是否抛出异常, 这里选择不需要
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);// 当HTTP的状态非200时是否抛出异常, 这里选择不需要
		webClient.getOptions().setActiveXNative(false);
		webClient.getOptions().setCssEnabled(false);// 是否启用CSS, 因为不需要展现页面, 所以不需要启用
		webClient.getOptions().setJavaScriptEnabled(true); // 很重要，启用JS。有些网站要开启！
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());// 很重要，设置支持AJAX
		webClient.getOptions().setTimeout(30000);
		webClient.waitForBackgroundJavaScript(3000);
		webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {
			@Override
			public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {
			}

			@Override
			public void scriptException(HtmlPage page, ScriptException scriptException) {
			}

			@Override
			public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {
			}

			@Override
			public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {
			}

			@Override
			public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {
				// TODO Auto-generated method stub

			}
		});
		return webClient;
	}

	public void close() {
		WebClient webClient = tl.get();
		if (webClient != null) {
			webClient.getCurrentWindow().getJobManager().removeAllJobs();
			webClient.close();
			System.gc();
		}
	}

	ThreadLocal<WebClient> tl = new ThreadLocal<WebClient>();
//	WebClient webclient = WebClientPoolFactory.getWebClient();

	public HtmlPage getHtmlPageFromUrl(String url, Map<String, String> header) {
		WebClient webclient = create();
		try {
			if (header != null) {
				header.keySet().forEach(key -> {
					webclient.addRequestHeader(key, header.get(key));
				});
			}
			tl.set(webclient);
			return webclient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return null;
		}
	}

	public HtmlPage getHtmlPageFromUrl(String url) {
		WebClient webclient = create();
		try {
			tl.set(webclient);
			return webclient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return null;
		}
	}

	public HtmlPage getHtmlPageFromUrlWithoutJs(String url, Map<String, String> header) {
		WebClient webclient = createWithoutJs();
		try {
			if (header != null) {
				header.keySet().forEach(key -> {
					webclient.addRequestHeader(key, header.get(key));
				});
			}
			tl.set(webclient);
			return webclient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return null;
		}
	}

	public HtmlPage getHtmlPageFromUrlWithoutJs(String url) {
		WebClient webclient = createWithoutJs();
		try {
			tl.set(webclient);
			return webclient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Http 页面异常，链接地址:" + url);
			return null;
		}
	}

	private WebClient createWithoutJs() {
		WebClient webClient = new WebClient();
		webClient = new WebClient(BrowserVersion.CHROME);// 新建一个模拟谷歌Chrome浏览器的浏览器客户端对象
		webClient.getOptions().setThrowExceptionOnScriptError(false);// 当JS执行出错的时候是否抛出异常, 这里选择不需要
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);// 当HTTP的状态非200时是否抛出异常, 这里选择不需要
		webClient.getOptions().setActiveXNative(false);
		webClient.getOptions().setCssEnabled(false);// 是否启用CSS, 因为不需要展现页面, 所以不需要启用
		webClient.getOptions().setJavaScriptEnabled(false); // 很重要，启用JS。有些网站要开启！
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());// 很重要，设置支持AJAX
		webClient.getOptions().setTimeout(3000);
		webClient.waitForBackgroundJavaScript(0);
		webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {
			@Override
			public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {
			}

			@Override
			public void scriptException(HtmlPage page, ScriptException scriptException) {
			}

			@Override
			public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {
			}

			@Override
			public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {
			}

			@Override
			public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {
				// TODO Auto-generated method stub

			}
		});
		return webClient;
	}
}
