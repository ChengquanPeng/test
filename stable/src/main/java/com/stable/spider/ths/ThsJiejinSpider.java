package com.stable.spider.ths;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.JiejinDao;
import com.stable.service.StockBasicService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ThsJiejinSpider {

	// http://data.eastmoney.com/dxf/q/601989.html

	private String URL_S = "http://basic.10jqka.com.cn/%s/equity.html";
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private JiejinDao jiejinDao;

	private String host = "http://basic.10jqka.com.cn";
	private Map<String, String> header;

	public void dofetch() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date());
					if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
						log.info("非周六");
						return;
					}
					log.info("周六");
					if (header == null) {
						header = new HashMap<String, String>();
						header.put("Referer", host);
						header.put("Upgrade-Insecure-Requests", "1");
					}
					List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
					List<Jiejin> savelist = new ArrayList<Jiejin>();
					for (StockBaseInfo b : list) {
						dofetch(b.getCode(), savelist);
						if (savelist.size() > 100) {
							jiejinDao.saveAll(savelist);
							savelist = new ArrayList<Jiejin>();
						}
					}
					if (savelist.size() > 0) {
						jiejinDao.saveAll(savelist);
					}
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-抓包解禁出错-抓包出错");
				}
			}
		}).start();
	}

	private void dofetch(String code, List<Jiejin> savelist) {
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(URL_S, code);
		ThreadsUtil.sleepRandomSecBetween1And5();
		do {
			try {
				log.info(url);
//				HtmlPage page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url, header);
				HtmlPage page = htmlunitSpider.getHtmlPageFromUrl(url, header);
				HtmlElement body = page.getBody();
//				System.err.println(body.asText());
				HtmlElement table = body.getElementsByAttribute("table", "class", "m_table m_hl ").get(0);
//				String res = table.asText();
//				System.err.println(res);
				Iterator<DomElement> it0 = table.getChildElements().iterator();
				it0.next();// thead
				DomElement tbody = it0.next();// tbody
				Iterator<DomElement> it1 = tbody.getChildElements().iterator();
				while (it1.hasNext()) {
					DomElement tr = it1.next();
					Iterator<DomElement> it2 = tr.getChildElements().iterator();
					Jiejin jj = new Jiejin();
					jj.setCode(code);
					jj.setDate(DateUtil.convertDate2(it2.next().asText()));// 解禁时间
					it2.next();// 公告解禁数量
					try {
						jj.setNum(CurrencyUitl.covertToLong(it2.next().asText()));// 解禁可售数量
					} catch (Exception e) {
						// e.printStackTrace();
					}
					try {
						jj.setZzb(Double.valueOf(it2.next().asText().replace("%", "")));// 解禁可售数量占比
					} catch (Exception e) {
						// e.printStackTrace();
					}
					jj.setType(it2.next().asText());// 解禁股份类型
					it2.next();// 前日收盘价
					try {
						jj.setCost(Double.valueOf(it2.next().asText()));// 估算成本
					} catch (Exception e) {
						// e.printStackTrace();
					}
					it2.next();// 是否公告值
//					log.info(jj);
					jj.setId(jj.getCode() + jj.getDate());
//					System.err.println(jj);
					savelist.add(jj);
				}
				fetched = true;
			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 5) {
					fetched = true;
					e2.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-解禁获取出错,url=" + url);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
	}

	public static void main(String[] args) {
		ThsJiejinSpider tp = new ThsJiejinSpider();
		tp.header = new HashMap<String, String>();
		tp.htmlunitSpider = new HtmlunitSpider();
		String[] as = { "601989", "603385", "300676", "002405", "601369", "600789", "002612" };
//		for (int i = 0; i < as.length; i++) {
//			tp.dofetch(as[i], new ArrayList<Jiejin>());
//		}
		tp.dofetch(as[1], new ArrayList<Jiejin>());
	}

}
