package com.stable.spider.ths;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.EsStockBaseInfoDao;
import com.stable.service.StockBasicService;
import com.stable.spider.eastmoney.EastmoneyCompanySpider;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 同花顺板块
 *
 */

@Component
@Log4j2
public class ThsPlateSpider {
	@Autowired
	private HtmlunitSpider htmlunitSpider;// = new HtmlunitSpider();
	@Autowired
	private EsStockBaseInfoDao esStockBaseInfoDao;
	private String BASE_URL = "http://basic.10jqka.com.cn/%s/";
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EastmoneyCompanySpider eastmoneyCompanySpider;

	public void fetchAll(boolean updateAll) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
					List<StockBaseInfo> upd = new LinkedList<StockBaseInfo>();
					int needUpd = 0;

					for (StockBaseInfo b : list) {
						boolean updateCache = false;
						if (updateAll || StringUtils.isBlank(b.getThsLightspot())
								|| StringUtils.isBlank(b.getThsMainBiz())) {
							needUpd++;
							if (dofetch(b)) {
								upd.add(b);
								updateCache = true;
							}
						}
						if (updateAll) {
							eastmoneyCompanySpider.getCompanyInfo(b);
						}
						if (updateAll || updateCache) {
							stockBasicService.synBaseStockInfo(b, true);
						}
					}
					if (upd.size() > 0) {
						esStockBaseInfoDao.saveAll(list);
					}
					if (needUpd > 0 && needUpd != upd.size()) {
						WxPushUtil.pushSystem1("同花顺-行业，亮点，主营-抓包不完整，需要更新数={" + needUpd + "},实际更新数={" + upd.size() + "}");
					}
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-亮点，主营-抓包出错");
				}
			}
		}).start();
	}

	private boolean dofetch(StockBaseInfo b) {
		String code = b.getCode();
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(BASE_URL, code);
		do {
			ThreadsUtil.sleepRandomSecBetween1And5();
			HtmlPage page = null;
			HtmlElement body = null;
			try {
				log.info(url);
				page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url);
				body = page.getBody();
				HtmlElement profile = body.getElementsByAttribute("div", "id", "profile").get(0);// profile
//				HtmlElement hy = profile.getElementsByAttribute("span", "class", "tip f14").get(0);// 所属申万行业
//				//System.err.println(hy.asText());
//				b.setThsIndustry(hy.asText().trim());
				HtmlElement e2 = profile.getElementsByAttribute("span", "class", "tip f14 fl core-view-text").get(0);// 公司亮点
				b.setThsLightspot(e2.asText().trim());
				DomElement e3 = profile.getElementsByAttribute("span", "class", "tip f14 fl main-bussiness-text").get(0)
						.getFirstElementChild();// 主营业务
				b.setThsMainBiz(e3.getAttribute("title"));

				if (StringUtils.isBlank(b.getThsLightspot()) || StringUtils.isBlank(b.getThsMainBiz())) {
					log.info("code={},getThsIndustry={},getThsLightspot={},getThsMainBiz={},trytime={}", code,
							b.getThsIndustry(), b.getThsLightspot(), b.getThsMainBiz(), trytime);
				} else {
					return true;
				}

			} catch (Exception e2) {
				e2.printStackTrace();
			} finally {
				htmlunitSpider.close();
			}

			trytime++;
			ThreadsUtil.sleepRandomSecBetween1And5(trytime);
			if (trytime >= 5) {
				fetched = true;
				WxPushUtil.pushSystem1("同花顺-亮点，主营出错出错code=" + code + ",url=" + url);
			}
		} while (!fetched);
		return false;
	}

	public static void main(String[] args) {
		ThsPlateSpider tp = new ThsPlateSpider();
		tp.htmlunitSpider = new HtmlunitSpider();
		// tp.dofetchHye(false);
		System.err.println();
	}
}
