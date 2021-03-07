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
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.HolderPercent5;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.HolderPercent;
import com.stable.vo.bus.MonitorPool;
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
	@Autowired
	private MonitorPoolService monitorPoolService;

	private String urlbase = "http://basic.10jqka.com.cn/%s/holder.html?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;

	public synchronized void dofetchHolder(boolean isWeekEnd) {
		try {
			List<String> codesw = null;
			if (!isWeekEnd) {
				List<MonitorPool> wlist = monitorPoolService.getHolderWarningList();
				codesw = new LinkedList<String>();
				if (wlist != null) {
					for (MonitorPool mp : wlist) {
						codesw.add(mp.getCode());
					}
				}
			}
			int sysdate = DateUtil.getTodayIntYYYYMMDD();
			dofetchHolderInner(sysdate, isWeekEnd, codesw);
			monitorPoolService.jobHolderWarning();
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "同花顺股东人数异常运行异常..", "", "");
			WxPushUtil.pushSystem1("同花顺股东人数异常运行异常");
		}
	}

	private void dofetchHolderInner(int sysdate, boolean isWeekEnd, List<String> codesw) {
		if (header == null) {
			header = new HashMap<String, String>();
		}

		// int chkdate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(sysdate + "",
		// -30));
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithOutSort();
		int c = 0;
		for (StockBaseInfo s : codelist) {
			try {
				String code = s.getCode();
				// 周末全量，redisUtil.get(RedisConstant.RDS_HOLDER_CODE_ + code, 0) <= chkdate
				// 预警，每天
				if ((isWeekEnd) || codesw.contains(code)) {
					if (stockBasicService.online2YearChk(code, sysdate)) {
						dofetchHolderInner(sysdate, code, hns, hps);
						if (hns.size() > 1000) {
							esHolderNumDao.saveAll(hns);
							esHolderPercentDao.saveAll(hps);
							hps = new LinkedList<HolderPercent>();
							hns = new LinkedList<HolderNum>();
						}
					}
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
			c++;
			log.info("current index:{}", c);
		}
		if (hns.size() > 0) {
			esHolderNumDao.saveAll(hns);
		}
		if (hps.size() > 0) {
			esHolderPercentDao.saveAll(hps);
		}
		WxPushUtil.pushSystem1(sysdate + " 股东人数/股东研究抓包同花顺已完成");
	}

	private void dofetchHolderInner(int sysdate, String code, List<HolderNum> hns, List<HolderPercent> hps) {
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

				// 股东人数变化
				HtmlElement boardInfos = body.getElementsByAttribute("div", "id", "gdrsFlashData").get(0);
				String res = boardInfos.asText();
				JSONArray jar = JSON.parseArray(res);
				for (int i = 0; i < jar.size(); i++) {
					JSONArray jo = jar.getJSONArray(i);
//					System.err.println(jo.get(0) + "," + jo.get(1) + "," + jo.get(2));
					HolderNum hn = new HolderNum();
					hn.setCode(code);
					try {
						if (jo.get(1) != null && jo.get(2) != null && !"false".equals(jo.get(2).toString())) {
							hn.setDate(DateUtil.convertDate2(jo.get(0).toString()));
							hn.setNum(Double.valueOf(jo.get(1).toString()).intValue());
							hn.setAvgPrice(Double.valueOf(jo.get(2).toString()));
							hn.setId(code + hn.getDate());
							hn.setSysdate(sysdate);
							hns.add(hn);
							fetched = true;

//							if (i == 0) {
//								redisUtil.set(RedisConstant.RDS_HOLDER_CODE_ + code, hn.getDate());
//							}
						}
					} catch (Exception e) {
						log.info(res);
						e.printStackTrace();
					}
				}

				// 十大股东
				HtmlElement bd_0 = body.getElementsByAttribute("div", "id", "bd_0").get(0);
				Iterator<DomElement> it0 = bd_0.getFirstElementChild().getFirstElementChild().getChildElements()
						.iterator();
				int j = 1;
				HolderPercent5 hp5 = new HolderPercent5();
				while (it0.hasNext()) {
					HtmlElement ther_x = null;
					String key = "ther_" + j;
					try {
						ther_x = body.getElementsByAttribute("div", "id", key).get(0);
						HolderPercent hp = holderAll(body, ther_x, code, hp5);
						hp.setCode(code);
						hp.setDate(DateUtil.convertDate2(it0.next().asText()));
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
					if (j == 1) {
						try {
							// 十大流通股东最新页
//						HtmlElement bd_1 = body.getElementsByAttribute("div", "id", "bd_1").get(0);
//						Iterator<DomElement> it1 = bd_1.getFirstElementChild().getFirstElementChild().getChildElements()
//								.iterator();
							key = "fher_" + j;
							HtmlElement fher_x = body.getElementsByAttribute("div", "id", key).get(0);
							ltbl(body, fher_x, code, hp5);
//						System.err.println("十大5%股东:");
//						for (String name : hp5.getList_a()) {
//							System.err.println(name);
//						}
							double circZb = 0.0;
							for (Double zb : hp5.getList_l()) {
								circZb += zb;
							}
							log.info("{} 十大5%股东:{}", code, circZb);
							stockBasicService.synBaseStockInfoCircZb(code, circZb);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					j++;
					if (j >= 10) {
						break;
					}
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

	private HolderPercent holderAll(HtmlElement body, HtmlElement ther_x, String code, HolderPercent5 hp5) {
		HolderPercent hp = new HolderPercent();
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
		while (it3.hasNext()) {
			i++;
			DomElement tr = it3.next();// th1
			Iterator<DomElement> it4 = tr.getChildElements().iterator();
//			it4.next();// 
			String name = it4.next().asText().trim();// 机构或基金名称
			it4.next();// 持有数量(股)
			it4.next();// 持股变化(股)
			String zb = it4.next().asText();// 占总股本比例
			double d = Double.valueOf(zb.replace("%", ""));
			if (i <= 3) {
				hp.addNum(d);
			}
			if (d >= 5.0) {
				hp5.getList_a().add(name);
			}

			if (i >= 10) {
				break;
			}
		}
		hp.cuteTopTotol();
		return hp;
	}

	private void ltbl(HtmlElement body, HtmlElement ther_x, String code, HolderPercent5 hp5) {
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
		while (it3.hasNext()) {
			DomElement tr = it3.next();// th1
			Iterator<DomElement> it4 = tr.getChildElements().iterator();
//			it4.next();//
			String name = it4.next().asText().trim();// 机构或基金名称
			it4.next();// 持有数量(股)
			it4.next();// 持股变化(股)
			if (hp5.getList_a().contains(name)) {
				String zb = it4.next().asText();// 占总股本比例
				hp5.getList_l().add(Double.valueOf(zb.replace("%", "")));
			}
		}
	}

	public static void main(String[] args) {
		ThsHolderSpider ts = new ThsHolderSpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		ts.dofetchHolderInner(DateUtil.getTodayIntYYYYMMDD(), "600340", hns, hps);
		for (HolderNum h : hns) {
			System.err.println(h);
		}
		for (HolderPercent h : hps) {
			System.err.println(h);
		}
	}
}
