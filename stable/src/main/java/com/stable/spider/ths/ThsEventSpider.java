package com.stable.spider.ths;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.service.StockBasicService;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 近期重要事件
 */
@Component
@Log4j2
public class ThsEventSpider {
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	private String urlbase = "https://basic.10jqka.com.cn/%s/event.html#stockpage?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;

	public void byJob() {
		dofetchInner();
	}

	public void byWeb() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				dofetchInner();
			}
		}).start();
	}

	public void dofetchInner() {
		try {
			dofetchInner2();
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "同花顺-近期重要事件-异常运行异常..", "", "");
			WxPushUtil.pushSystem1("同花顺-近期重要事件-异常运行异常");
		}
	}

	private synchronized void dofetchInner2() {
		if (header == null) {
			header = new HashMap<String, String>();
		}
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		int c = 0;
		for (StockBaseInfo s : codelist) {
			try {
				dofetchInner3(s.getCode());
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
			c++;
			log.info("current index:{}", c);
		}
		log.info("同花顺-近期重要事件-done");
	}

	private void dofetchInner3(String code) {
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(urlbase, code, System.currentTimeMillis());
		ThreadsUtil.sleepRandomSecBetween5And15Ths();
		do {
			try {
				log.info(url);
				header.put("Referer", host + code + "/");
				HtmlPage page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url, header);
				HtmlElement body = page.getBody();
				// System.err.println(body.asText());
				HtmlElement table = body.getElementsByAttribute("table", "id", "tableList").get(0);
				DomElement tbody = table.getFirstElementChild();

				Iterator<DomElement> it0 = tbody.getChildElements().iterator();
				while (it0.hasNext()) {
					DomElement tr = it0.next();// 经营性质
					DomElement td1 = tr.getFirstElementChild();
					DomElement td2 = tr.getLastElementChild();
					if (!td2.asText().startsWith("参控公司")) {
						System.err.println(td1.asText() + ":" + td2.asText());
					}
				}
				fetched = true;
			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					fetched = true;
					e2.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-近期重要事件-获取出错,url=" + url);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
	}

	public static void main(String[] args) {
		ThsEventSpider ts = new ThsEventSpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		ts.dofetchInner3("002298");

	}
}
