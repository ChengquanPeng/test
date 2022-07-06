package com.stable.spider.ths;

import java.util.Collection;
import java.util.Comparator;
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
import com.stable.msg.WxPushUtil;
import com.stable.service.ChipsService;
import com.stable.service.StockBasicService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.HolderPercent5;
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
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private ChipsService chipsService;

	private String urlbase = "http://basic.10jqka.com.cn/%s/holder.html?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;

	public synchronized void dofetchHolder(boolean isWeekEnd) {
		try {
			Collection<String> codesw = new LinkedList<String>();
			if (isWeekEnd) {
				List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithOutSort();
				if (codelist != null) {
					for (StockBaseInfo s : codelist) {
						codesw.add(s.getCode());
					}
				}
			} else {
				codesw = monitorPoolService.getListForFetchHolder();
			}
			log.info("codesw size={},isWeekEnd={}", codesw.size(), isWeekEnd);
			int sysdate = DateUtil.getTodayIntYYYYMMDD();
			dofetchHolderInner(sysdate, codesw);
			monitorPoolService.jobHolderWarning();
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "同花顺股东人数异常运行异常..", "", "");
			WxPushUtil.pushSystem1("同花顺股东人数异常运行异常");
		}
	}

	public void doCodeFe(String code) {
		header = new HashMap<String, String>();
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		dofetchHolderInner(DateUtil.getTodayIntYYYYMMDD(), code, hns, hps);
		if (hns.size() > 0) {
			esHolderNumDao.saveAll(hns);
		}
		if (hps.size() > 0) {
			esHolderPercentDao.saveAll(hps);
		}
		log.info(code + " 股东人数/股东研究抓包同花顺已完成");
	}

	private void dofetchHolderInner(int sysdate, Collection<String> codesw) {
		if (codesw.size() <= 0) {
			return;
		}
		if (header == null) {
			header = new HashMap<String, String>();
		}
		// int chkdate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(sysdate + "",
		// -30));
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		int pre2Year = DateUtil.getPreYear(sysdate, 2);
		int c = 0;
		for (String code : codesw) {
			c++;
			try {
				log.info("current index:{},{}", c, code);
				// 预警，每天
				if (stockBasicService.onlinePreYearChk(code, pre2Year)) {
					dofetchHolderInner(sysdate, code, hns, hps);
					if (hns.size() > 1000) {
						esHolderNumDao.saveAll(hns);
						esHolderPercentDao.saveAll(hps);
						hps = new LinkedList<HolderPercent>();
						hns = new LinkedList<HolderNum>();
					}
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
		log.info("所有股东人数/股东研究抓包同花顺已完成");
		// WxPushUtil.pushSystem1(sysdate + " 股东人数/股东研究抓包同花顺已完成");
	}

	public void re() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				reinit();
			}
		}).start();
	}

	public void reinit() {
		log.info("所有股东人数/股东研究-重新计算start");
		List<HolderNum> hns = new LinkedList<HolderNum>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithOutSort();
		int c = 0;
		for (StockBaseInfo s : codelist) {
			c++;
			try {
				String code = s.getCode();
				log.info("current index:{},{}", c, code);
				HolderNum hn = cutAvgPrcent(code, chipsService.getLastHolderPercent(code).getTop10circZb(),
						chipsService.getHolderNumList45(code));
				if (hn != null) {
					hns.add(hn);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		if (hns.size() > 0) {
			esHolderNumDao.saveAll(hns);
		}
		log.info("所有股东人数/股东研究抓包同花顺已完成-重新计算");
		WxPushUtil.pushSystem1("所有股东人数/股东研究抓包同花顺已完成-重新计算");
	}

	private HolderNum cutAvgPrcent(String code, Double top10Zb, List<HolderNum> hns) {
		if (top10Zb != null && hns != null && hns.size() > 0) {
			HolderNum maxn = hns.stream().max(Comparator.comparing(HolderNum::getDate)).get();
			if (maxn.getNum() <= 0) {
				log.warn("{} 计算实际人均持股错误，Num()<=0", code);
				return null;
			}
			if (top10Zb <= 0.0) {
				log.warn("{} 计算实际人均持股错误，top10的持股股东数据为空", code);
				maxn.setAvgNumP5(0);
				return maxn;
			}
			log.info("{} top10Zb={}", code, top10Zb);
			StockBaseInfo sb = stockBasicService.getCode(code);
			long fs = CurrencyUitl.covertToLong(sb.getFloatShare() + CurrencyUitl.YI);// 流通股份

			// 人均：出去5%股东的人均(100-5%股东持股）x流通股数 除以股东人数
			// 除以100是 p5circZb 百分比
			long avgNum = Double.valueOf((((100 - top10Zb) * fs / 100) / maxn.getNum())).longValue();
			maxn.setAvgNumP5(avgNum);
			log.warn("{} 计算实际流通股份{},散户占比：{}%,股东人数({}):{}  人均持股: {}", code, sb.getFloatShare(), (100 - top10Zb),
					maxn.getDate(), maxn.getNum(), avgNum);
			return maxn;
		} else {
			log.warn("{} 计算实际人均持股错误，股东数据为空", code);
		}
		return null;
	}

	private void dofetchHolderInner(int sysdate, String code, List<HolderNum> hnsx, List<HolderPercent> hpsx) {
		List<HolderNum> hnsl = new LinkedList<HolderNum>();
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(urlbase, code, System.currentTimeMillis());
		ThreadsUtil.sleepRandomSecBetween5And15Ths();
		do {
			try {
				// log.info(url);
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
							hnsl.add(hn);
							hnsx.add(hn);
							fetched = true;
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
				HolderPercent5 hp5 = new HolderPercent5();// 5%以上的股东名字
				while (it0.hasNext()) {
					HtmlElement ther_x = null;
					String key = "ther_" + j;
					try {
						ther_x = body.getElementsByAttribute("div", "id", key).get(0);
						HolderPercent hp = holderAll(body, ther_x, code, hp5);// 获取5%的名字
						hp.setCode(code);
						hp.setDate(DateUtil.convertDate2(it0.next().asText()));
						hp.setId(code + hp.getDate());
						hp.setSysdate(sysdate);
						hpsx.add(hp);
						fetched = true;

						if (j == 1) {
							key = "fher_" + j;
							HtmlElement fher_x = null;
							try {
								// 十大流通股东最新页
//							HtmlElement bd_1 = body.getElementsByAttribute("div", "id", "bd_1").get(0);
//							Iterator<DomElement> it1 = bd_1.getFirstElementChild().getFirstElementChild().getChildElements()
//									.iterator();
								fher_x = body.getElementsByAttribute("div", "id", key).get(0);
								double top10Zb = ltbl(body, fher_x, code, hp5);
//							System.err.println("十大5%股东:");
//							for (String name : hp5.getList_a()) {
//								System.err.println(name);
//							}
								double p5circZb = 0.0;
								for (Double zb : hp5.getList_l()) {
									p5circZb += zb;
								}
								log.info("{} 十大5%股东流通股占比:{},Top10股东流通股占比:{}", code, p5circZb, top10Zb);
								hp.setPercent5circZb(p5circZb);
								hp.setTop10circZb(top10Zb);
								cutAvgPrcent(code, top10Zb, hnsl);
								stockBasicService.synBaseStockInfoCircZb(code, p5circZb);
							} catch (Exception e) {
								log.info(key);
								if (fher_x != null) {
									// log.info(fher_x.asXml());
								}
								e.printStackTrace();
							}
						}
						hp.cuteTopTotol();
					} catch (Exception e) {
						log.info(key);
						if (ther_x != null) {
							log.info(ther_x.asXml());
						}
						e.printStackTrace();
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

	// 10大股东名单
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
		int i = 0;
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
			if (i == 1) {// 第一大股东
				hp.setTop1(d);
			}
			if (i <= 3) {// 前三大股东
				hp.addTop3(d);
			}
			if (d >= 5.0) {// 持股5%股东
				hp.addPercent5(d);
				hp5.getList_a().add(name);
			}

			if (i >= 10) {
				break;
			}
		}
		return hp;
	}

	// 流通股东
	private double ltbl(HtmlElement body, HtmlElement ther_x, String code, HolderPercent5 hp5) {
//		Iterator<DomElement> it = ther_x.getChildElements().iterator();
//		it.next();// div
//		DomElement table = it.next();// table
		DomElement table = ther_x.getElementsByTagName("table").get(0);
		Iterator<DomElement> it2 = table.getChildElements().iterator();
		it2.next();// caption
		it2.next();// thead
		// tbody
		DomElement tbody = it2.next();
//			System.err.println(tbody.getTagName());
//			System.err.println(ther_x.asXml());
		Iterator<DomElement> it3 = tbody.getChildElements().iterator();
		double p10Zb = 0.0;
		while (it3.hasNext()) {
			DomElement tr = it3.next();// th1
			Iterator<DomElement> it4 = tr.getChildElements().iterator();
//				it4.next();//
			String name = it4.next().asText().trim();// 机构或基金名称
			it4.next();// 持有数量(股)
			it4.next();// 持股变化(股)
			String zb = it4.next().asText();// 占流通股比例
			double zbd = Double.valueOf(zb.replace("%", ""));
			if (hp5.getList_a().contains(name)) {// 涉及到股权转让，则按名字来判断不准。应该按总股本减去/持股比例
				hp5.getList_l().add(zbd);
			}
			p10Zb += zbd;
		}
		return p10Zb;
	}

	public static void main(String[] args) {
		ThsHolderSpider ts = new ThsHolderSpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		List<HolderPercent> hps = new LinkedList<HolderPercent>();
		List<HolderNum> hns = new LinkedList<HolderNum>();
		ts.dofetchHolderInner(DateUtil.getTodayIntYYYYMMDD(), "688599", hns, hps);
		for (HolderNum h : hns) {
			System.err.println(h);
		}
		for (HolderPercent h : hps) {
			System.err.println(h);
		}
	}
}
