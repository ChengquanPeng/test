package com.stable.spider.sina;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.constant.Constant;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.TheadUtil;

@Component
public class SinaSpider {

	@Autowired
	private HtmlunitSpider htmlunitSpider;

	private static final String HISTORY_INDEX_URL = "http://money.finance.sina.com.cn/corp/go.php/vMS_MarketHistory/stockid/%s.phtml";
	private static final String HISTORY_PAGES_URL = "http://money.finance.sina.com.cn/corp/go.php/vMS_MarketHistory/stockid/%s.phtml?year=%s&jidu=%s";

	public List<String> getDaliyTradyHistory(String code) {
		List<String> data = new LinkedList<String>();
		String url = String.format(HISTORY_INDEX_URL, code);
		String years = fetchHistroyPage(url, data);
		if (years == null) {
			return null;
		}
		addPages(code, data, years, DateUtil.getCurYYYY(), DateUtil.getCurJidu());
		return data;
	}

	private void addPages(String code, List<String> data, String years, int year, int jidu) {
		jidu = jidu - 1;
		if (jidu == 0) {
			jidu = 4;
			year = year - 1;
		}
		if (!years.contains(year + "")) {
			return;
		}
		String url = String.format(HISTORY_PAGES_URL, code, year, jidu);
		fetchHistroyPage(url, data);
		addPages(code, data, years, year, jidu);
	}

	private String fetchHistroyPage(String url, List<String> data) {
		try {
			HtmlPage page = htmlunitSpider.getHtmlPageFromUrl(url);
			DomElement table = page.getElementById("FundHoldSharesTable");
			DomElement year = page.getElementByName("year");

			// year
			StringBuilder y = new StringBuilder();
			year.getChildren().forEach(opt -> {
				if (StringUtils.isNotBlank(opt.asText())) {
					opt.getChildren().forEach(v -> {
						if (StringUtils.isNotBlank(v.asText())) {
							// System.err.print(v.asText()+",");
							y.append(v.asText()).append(Constant.DOU_HAO);
						}
					});
				}
			});

			// data
			if (table != null) {//未获取到数据，停牌或者被屏蔽
				//3：数据栏
				table.getChildNodes().get(3).getChildNodes().forEach(tr -> {
					if (StringUtils.isNotBlank(tr.asText())) {
						StringBuilder d = new StringBuilder();
						tr.getChildren().forEach(td -> {
							if (StringUtils.isNotBlank(td.asText())) {
								// System.err.print(td.asText()+",");
								d.append(td.asText()).append(Constant.DOU_HAO);
							}
						});
						data.add(d.toString());
					}
				});
			}

			return y.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			TheadUtil.sleepRandomSecBetween5And15();
		}
	}
}
