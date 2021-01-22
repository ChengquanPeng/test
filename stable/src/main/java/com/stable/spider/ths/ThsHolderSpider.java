package com.stable.spider.ths;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.EsHolderNumDao;
import com.stable.es.dao.base.EsHolderPercentDao;
import com.stable.service.StockBasicService;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.HolderPercent;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 股东研究
 */
@Component
@Log4j2
public class ThsHolderSpider {
	@Autowired
	private EsHolderNumDao esHolderNumDao;
	@Autowired
	private EsHolderPercentDao esHolderPercentDao;
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	private String urlbase = "http://basic.10jqka.com.cn/%s/holder.html?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;

	public void dofetchHolder() {
		try {
			int sysdate = DateUtil.getTodayIntYYYYMMDD();
			dofetchHolderInner(sysdate);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "同花顺股东人数异常运行异常..", "", "");
			WxPushUtil.pushSystem1("同花顺股东人数异常运行异常");
		}
	}

	private void dofetchHolderInner(int sysdate) {
		if (header == null) {
			header = new HashMap<String, String>();
		}
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : codelist) {
			try {
				dofetchHolderInner(sysdate, s.getCode(), hns, hps);
				if (hns.size() > 1000) {
					esHolderNumDao.saveAll(hns);
					esHolderPercentDao.saveAll(hps);
					hps = new LinkedList<HolderPercent>();
					hns = new LinkedList<HolderNum>();
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		if (hns.size() > 0) {
			esHolderNumDao.saveAll(hns);
		}
		if (hps.size() > 0) {
			esHolderPercentDao.saveAll(hps);
		}
		WxPushUtil.pushSystem1(sysdate + " 股东研究抓包同花顺已完成");
	}

	private void dofetchHolderInner(int sysdate, String code, List<HolderNum> hns, List<HolderPercent> hps) {
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(urlbase, code, System.currentTimeMillis());
		ThreadsUtil.sleepRandomSecBetween1And5();
		do {
			try {
				log.info(url);
				header.put("Referer", host + code + "/");
				HtmlPage page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url, header);
				HtmlElement body = page.getBody();
				// System.err.println(body.asText());
				HtmlElement boardInfos = body.getElementsByAttribute("div", "id", "gdrsFlashData").get(0);
				String res = boardInfos.asText();
				JSONArray jar = JSON.parseArray(res);
				for (int i = 0; i < jar.size(); i++) {
					JSONArray jo = jar.getJSONArray(i);
//					System.err.println(jo.get(0) + "," + jo.get(1) + "," + jo.get(2));
					HolderNum hn = new HolderNum();
					hn.setCode(code);
					try {
						hn.setDate(DateUtil.convertDate2(jo.get(0).toString()));
						hn.setNum(Double.valueOf(jo.get(1).toString()).intValue());
						hn.setAvgPrice(Double.valueOf(jo.get(2).toString()));
						hn.setId(code + hn.getDate());
						hn.setSysdate(sysdate);
						hns.add(hn);
						fetched = true;
					} catch (Exception e) {
						log.info(res);
						e.printStackTrace();
					}
				}
				HtmlElement bd_0 = body.getElementsByAttribute("div", "id", "bd_0").get(0);
				Iterator<DomElement> it0 = bd_0.getFirstElementChild().getFirstElementChild().getChildElements()
						.iterator();
				int j = 1;
				while (it0.hasNext()) {
					HolderPercent hp = new HolderPercent();
					hp.setCode(code);
					hp.setDate(DateUtil.convertDate2(it0.next().asText()));
					HtmlElement ther_x = null;
					String key = "ther_" + j;
					try {
						ther_x = body.getElementsByAttribute("div", "id", key).get(0);
						v1(body, ther_x, code, hp);
						hp.setId(code + hp.getDate());
						hp.setSysdate(sysdate);
						hps.add(hp);
						fetched = true;
					} catch (Exception e) {
						log.info(key);
						if (ther_x != null) {
							log.info(ther_x.asXml());
						}
						e.printStackTrace();
					}
					j++;
				}

			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					fetched = true;
					e2.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-股东获取出错,url=" + url);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
	}

	private void v1(HtmlElement body, HtmlElement ther_x, String code, HolderPercent hp) {
		Iterator<DomElement> it = ther_x.getChildElements().iterator();
		it.next();// div
		DomElement table = it.next();// table
		Iterator<DomElement> it2 = table.getChildElements().iterator();
		it2.next();// caption
		it2.next();// thead
		// tbody
		DomElement tbody = it2.next();
//		System.err.println(tbody.getTagName());
//		System.err.println(ther_x.asXml());
		Iterator<DomElement> it3 = tbody.getChildElements().iterator();
		int i = 1;
		while (it3.hasNext() && i <= 3) {
			i++;
			DomElement tr = it3.next();// th1
			Iterator<DomElement> it4 = tr.getChildElements().iterator();
			it4.next();// String name = it4.next().asText();// 机构或基金名称
			it4.next();// 持有数量(股)
			it4.next();// 持股变化(股)
			String zb = it4.next().asText();// 占总股本比例
			hp.addNum(Double.valueOf(zb.replace("%", "")));
		}
		hp.cuteTopTotol();
	}

	public static void main(String[] args) {
		ThsHolderSpider ts = new ThsHolderSpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		ts.dofetchHolderInner(DateUtil.getTodayIntYYYYMMDD(), "002988", hns, hps);
		for (HolderNum h : hns) {
			System.err.println(h);
		}
		for (HolderPercent h : hps) {
			System.err.println(h);
		}
	}
}
