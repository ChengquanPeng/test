package com.stable.spider.ths;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDailyDao;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.service.TradeCalService;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.ConceptDaily;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ThsSpider {
	private static final String BRK = "跳过首发新股";
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private EsConceptDao esConceptDao;
	@Autowired
	private EsCodeConceptDao esCodeConceptDao;
	@Autowired
	private EsConceptDailyDao esConceptDailyDao;
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	// private static final String FIN_RPT_URL =
	// "http://basic.10jqka.com.cn/%s/finance.html";

	private String GN_LIST = "http://q.10jqka.com.cn/gn/index/field/addtime/order/desc/page/%s/ajax/1/";
	private static int ths = 1;// 同花顺概念
	private static int thsHye = 2;// 同花顺行业
	private static String START_THS = "THS";
	private static String SPIT = "/";
//	private static Map<String, Concept> allmap = new HashMap<String, Concept>();
//	static {
//		for (int i = 0; i < ConAll.all.length; i++) {
//			String[] string = ConAll.all[i].split(",");
//			String url = string[0];
//			String name = string[1];
//			List<String> ids = Arrays.asList(url.split(SPIT));
//			Concept cp = new Concept();
//			cp.setCode(ids.get(ids.size() - 1));
//			cp.setId(START_THS + cp.getCode());
//			cp.setType(ths);
//			cp.setDate(20100101);
//			cp.setHref(url);
//			cp.setName(name);
//			allmap.put(cp.getCode(), cp);
//		}
//	}

//	private String host = "http://127.0.0.1:8081";
//	private String host = "http://106.52.95.147:9999";
//	private String url0 = host + "/web/concept/allConcepts";
//	private String url1 = host + "/web/concept/addConcept";
//	private String url2 = host + "/web/concept/addCodeConcept";
//	private String url3 = host + "/web/concept/addConceptDaily";

	private void saveConceptDaily(List<ConceptDaily> list) {
		if (list.size() > 0) {
			esConceptDailyDao.saveAll(list);
			list.clear();
		}
	}

	private void saveCodeConcept(List<CodeConcept> list) {
		if (list.size() > 0) {
			esCodeConceptDao.saveAll(list);
			list.clear();
		}
	}

	public void saveConcept(List<Concept> list) {
		if (list.size() > 0) {
			int update = DateUtil.getTodayIntYYYYMMDD();
			for (Concept cp : list) {
				cp.setUpdateDate(update);
			}
			esConceptDao.saveAll(list);
			list.clear();
		}
	}

	private void deleteAllCodeConcept() {
		esCodeConceptDao.deleteAll();
	}

	private Map<String, Concept> getAllAliasCode() {
		Map<String, Concept> m = new HashMap<String, Concept>();
		esConceptDao.findAll().forEach(x -> {
			if (StringUtils.isNotBlank(x.getAliasCode()) && !"null".equals(x.getAliasCode())) {
				m.put(x.getCode(), x);
				log.info(x);
			}
		});
		log.info(m.size());
		return m;
	}

	public void start() {
		int date = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		if (!tradeCalService.isOpen(date)) {
			log.info("非交易日");
			return;
		}
		Date today = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(today);
		int weekday = c.get(Calendar.DAY_OF_WEEK);
		boolean isFirday = true;
		if (weekday != 6) {
			log.info("今日非周五");
			isFirday = false;
		} else {
			deleteAllCodeConcept();
		}
		Map<String, Concept> m = synchGnAndCode(isFirday);
		if (m == null) {
			try {
				Thread.sleep(Duration.ofMinutes(5).toMillis());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			m = getAllAliasCode();
		}
		synchConceptDaliy(date, m);
		try {
			Thread.sleep(Duration.ofMinutes(5).toMillis());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dofetchHye(isFirday);
	}

	private void synchConceptDaliy(int date, Map<String, Concept> m) {
		try {
			int c = 0;
			List<String> keys = new ArrayList<String>(m.keySet());
			for (int i = 0; i < keys.size(); i++) {

				Concept cp = m.get(keys.get(i));
				log.info("抓包：" + cp.getName());
				if (BRK.contains(cp.getName())) {
					log.info(BRK + ">>" + cp.getName());
					// 首发新股
					continue;
				}
				c += getConceptDaily(cp, cp.getHref(), date);
			}
//			int c = list.size();
			log.info("同花顺板块交易记录同步成功,需求抓取总是[" + keys.size() + "],实际成功总数:[" + c + "]");
		} catch (Exception e) {
			// saveConceptDaily(list);
			e.printStackTrace();
			WxPushUtil.pushSystem1("同花顺概念-每日交易出错 end");
			throw new RuntimeException(e);
		}
	}

	public int getConceptDaily(Concept cp, String url, int date) {
		List<ConceptDaily> list = new LinkedList<ConceptDaily>();
		int trytime = 0;
		boolean fetched = false;
		do {
			ThreadsUtil.sleepRandomSecBetween5And15();
			HtmlPage page = null;
			HtmlElement body = null;
			try {
				log.info(url);
				page = htmlunitSpider.getHtmlPageFromUrl(url);
				body = page.getBody();
				HtmlElement boardInfos = body.getElementsByAttribute("div", "class", "board-infos").get(0);
				Iterator<DomElement> it = boardInfos.getChildElements().iterator();
				ConceptDaily cd = new ConceptDaily();
				cd.setOpen(Double.valueOf(it.next().getLastElementChild().asText()));// 今开
				cd.setYesterday(Double.valueOf(it.next().getLastElementChild().asText()));// 昨收
				cd.setLow(Double.valueOf(it.next().getLastElementChild().asText()));// 最低
				cd.setHigh(Double.valueOf(it.next().getLastElementChild().asText()));// 最高
				cd.setVol(Double.valueOf(it.next().getLastElementChild().asText()));// 成交量(万手)
				cd.setTodayChange(Double.valueOf(it.next().getLastElementChild().asText().replace("%", "")));// 板块涨幅
				cd.setRanking(Integer.valueOf(it.next().getLastElementChild().asText().split(SPIT)[0]));// 涨幅排名
				cd.setUpdownNum(it.next().getLastElementChild().asText());// 涨跌家数
				cd.setInComeMoney(Double.valueOf(it.next().getLastElementChild().asText()));// 资金净流入(亿)
				cd.setAmt(Double.valueOf(it.next().getLastElementChild().asText()));// 成交额(亿)
				Iterator<DomElement> it2 = body.getElementsByAttribute("div", "class", "board-hq").get(0)
						.getChildElements().iterator();
				it2.next();
				cd.setClose(Double.valueOf(it2.next().asText()));// 收盘
				fetched = true;
				cd.setConceptId(cp.getId());
				cd.setDate(date);
				cd.setId();
				log.info(cd);
				list.add(cd);
			} catch (Exception e2) {
				boolean esistingHeading = false;
				if (body != null) {
					try {
						body.getElementsByAttribute("div", "class", "heading").get(0);
						esistingHeading = true;// 概念下线导致的异常
					} catch (Exception e) {
					}
				}
				if (!esistingHeading) {
					e2.printStackTrace();
					trytime++;
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					if (trytime >= 10) {
						fetched = true;
						e2.printStackTrace();
						WxPushUtil.pushSystem1("同花顺概念-每日交易出错," + cp.getName() + ",url=" + url);
					}
				} else {
					fetched = true;
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
		saveConceptDaily(list);
		return list.size();
	}

	private Map<String, Concept> synchGnAndCode(boolean isFirday) {
		Map<String, Concept> map = null;
		try {
			map = getAllAliasCode();
			getGnList(isFirday, map);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("同花顺板块出错");
			map = null;
		}
		return map;
	}

	public void getGnList(boolean isFirday, Map<String, Concept> map) {
		List<Concept> list = new LinkedList<Concept>();
		int cntList = 0;
		int cntCodelist = 0;
		int limit = 1;

		StringBuffer newGn = new StringBuffer();

		int index = 1;
		int end = 0;
		int trytime = 0;
		do {
			String url = String.format(GN_LIST, index);
			DomElement table = null;
			HtmlPage page = null;
			try {
				ThreadsUtil.sleepRandomSecBetween5And15();
				page = htmlunitSpider.getHtmlPageFromUrl(url);
				table = page.getBody().getFirstElementChild();

				DomElement tbody = table.getLastElementChild();
				Iterator<DomElement> trs = tbody.getChildElements().iterator();
				while (trs.hasNext()) {
					Iterator<DomElement> tr = trs.next().getChildElements().iterator();
					Concept cp = new Concept();
					cp.setDate(Integer.valueOf(
							DateUtil.formatYYYYMMDD(DateUtil.parseDate(tr.next().asText(), DateUtil.YYYY_MM_DD2))));
					DomElement td2 = tr.next();
					cp.setName(td2.asText());// name
					String href = td2.getFirstElementChild().getAttribute("href");
					cp.setHref(href);
					tr.next();// 驱动事件
					tr.next();// 龙头股
					try {
						cp.setCnt(Integer.valueOf(tr.next().asText()));// 成分股数量
					} catch (Exception e) {
						e.printStackTrace();
					}
					List<String> ids = Arrays.asList(cp.getHref().split(SPIT));
					cp.setCode(ids.get(ids.size() - 1));
					cp.setId(START_THS + cp.getCode());
					cp.setType(ths);

					log.info(cp);
					boolean fetchNext = true;

					if (!isFirday) {// 周一到周4只需要获取新概念，周五就重新获取概率下所有股票
						// 每天新增
						if (map.containsKey(cp.getCode())) {
							fetchNext = false;
						} else {
							newGn.append(cp.getName()).append(",");
							log.info("获取到新概念:" + cp.getName());
							fetchNext = true;
						}
					}
					if (isFirday || fetchNext) {
						getAliasCdoe(cp, map);
						cntCodelist += getSubCodeList(GN_CODE_LIST, cp, ths);
						list.add(cp);
						if (list.size() >= 100) {
							cntList += list.size();
							saveConcept(list);
						}
					}
				}
				index++;
				if (end == 0) {
					String pageInfo = page.getElementById("m-page").getLastElementChild().asText();
					end = Integer.valueOf(pageInfo.split(SPIT)[1]);
				}
				trytime = 0;

				if (!isFirday) {
					if (index > limit) {// 最多3页
						break;
					}
				}
			} catch (Exception e) {
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("同花顺概念-列表抓包出错,url=" + url);
					if (list.size() > 0) {
						cntList += list.size();
						saveConcept(list);
					}
					throw new RuntimeException(e);
				}

			} finally {
				htmlunitSpider.close();
			}
		} while (index <= end);

//		try {
//			for (String id : allmap.keySet()) {
//				log.info("!map.contains name={},? {}", allmap.get(id).getName(), (!map.containsKey(id)));
//				if (!map.containsKey(id)) {
//					Concept cp = allmap.get(id);
//					getAliasCdoe(cp, map);
//					getSubCodeList(GN_CODE_LIST, cp, codelist, ths);
//					list.add(cp);
//					log.info("获取到新概念:" + cp.getName());
//				}
//			}
//		} finally {
//			
//		}
		if (list.size() > 0) {
			cntList += list.size();
			saveConcept(list);
		}
		if (newGn.length() > 0) {
			WxPushUtil.pushSystem1("同花顺获取到新概念:" + newGn.toString());
		}
		log.info("同花顺板块同步成功,同步概念[" + cntList + "],概念相关股票[" + cntCodelist + "]"
				+ (newGn.length() > 0 ? ",获取到新概念:" + newGn.toString() : ""));
	}

	private String GN_CODE_LIST = "http://q.10jqka.com.cn/gn/detail/field/264648/order/desc/page/%s/ajax/1/code/%s";

	private void getAliasCdoe(Concept cp, Map<String, Concept> map) {
		if (map.containsKey(cp.getCode())) {
			cp.setAliasCode(map.get(cp.getCode()).getAliasCode());
			return;
		}
		ThreadsUtil.sleepRandomSecBetween5And15();

		HtmlPage page = null;
		try {
			page = htmlunitSpider.getHtmlPageFromUrl(cp.getHref());
			String aliasCode = page.getElementById("clid").getAttribute("value");
			log.info("{} get AliasCode code:{}", cp.getName(), aliasCode);
			if (StringUtils.isNotBlank(aliasCode) && !"null".equals(aliasCode)) {
				cp.setAliasCode(aliasCode);
				map.put(cp.getCode(), cp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("同花顺概念-成分股抓包出错,url=" + cp.getHref());
			throw new RuntimeException(e);
		} finally {
			htmlunitSpider.close();
		}
	}

	public int getSubCodeList(String urlbase, Concept cp, int type) {
		List<CodeConcept> codelist = new LinkedList<CodeConcept>();
		int updateTime = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		int index = 1;
		int end = 0;
		int trytime = 0;
		int stcnt = 0;
		do {
			ThreadsUtil.sleepRandomSecBetween5And15();

			String url = String.format(urlbase, index, cp.getCode());
			log.info(url);
			DomElement table = null;
			HtmlPage page = null;
			try {
//				header.put(REFERER, cp.getHref());
				page = htmlunitSpider.getHtmlPageFromUrl(url);
				table = page.getBody().getFirstElementChild();
				DomElement tbody = table.getLastElementChild();
				if (tbody.asText().contains("暂无成份股数据")) {
					return 0;
				}
				Iterator<DomElement> trs = tbody.getChildElements().iterator();
				while (trs.hasNext()) {
					Iterator<DomElement> tr = trs.next().getChildElements().iterator();
					tr.next();// 序号

					CodeConcept cc = new CodeConcept();
					cc.setUpdateTime(updateTime);
					cc.setCode(tr.next().asText());// code
					cc.setConceptId(cp.getId());
					cc.setConceptName(cp.getName());
					cc.setType(type);
					cc.setId(cp.getId() + cc.getCode());
					codelist.add(cc);
					log.info(cc);
					stcnt++;
				}
				index++;
				if (end == 0) {
					String pageInfo = page.getElementById("m-page").getLastElementChild().asText();
					end = Integer.valueOf(pageInfo.split(SPIT)[1]);
				}
				trytime = 0;
				cp.setCnt(stcnt);
			} catch (Exception e) {
				// e.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					e.printStackTrace();
					log.info(page.asText());
					WxPushUtil.pushSystem1("同花顺行业概念-成分股抓包出错,url=" + url);
					// throw new RuntimeException(e);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (index <= end);
		if (codelist.size() > 0) {
			saveCodeConcept(codelist);
		}
		return codelist.size();
	}

	private String urlb = "http://q.10jqka.com.cn/thshy/index/field/199112/order/desc/page/%s/ajax/1/";
	private String urlSubBase = "http://q.10jqka.com.cn/thshy/detail/field/199112/order/desc/page/%s/ajax/1/code/%s";

	public void dofetchHye(boolean isFirday) {
		List<Concept> list = new LinkedList<Concept>();
		int date = DateUtil.getTodayIntYYYYMMDD();
		int j = 1;
		boolean isbraek = false;
		for (;;) {// 翻页
			int trytime = 0;
			boolean fetched = false;
			String url = String.format(urlb, j);
			do {
				HtmlPage page = null;
				HtmlElement body = null;
				try {
					log.info(url);
					page = htmlunitSpider.getHtmlPageFromUrl(url);
					body = page.getBody();// table
					DomElement tbody = body.getFirstElementChild().getLastElementChild();
					if (tbody.getChildElementCount() > 0) {
						Iterator<DomElement> it = tbody.getChildElements().iterator();
						while (it.hasNext()) {
							DomElement tr = it.next();
							// 序号 板块 涨跌幅(%) 总成交量（万手） 总成交额（亿元） 净流入（亿元） 上涨家数 下跌家数 均价 领涨股 最新价 涨跌幅(%)
							Iterator<DomElement> tds = tr.getChildElements().iterator();
							tds.next();// 序号
							DomElement td1 = tds.next();// 板块名字/连接
							String name = td1.asText();
							String href = td1.getFirstElementChild().getAttribute("href");
							String code = href.split("code")[1].replaceAll("/", "");
							// System.err.println(href + " " + code + " " + name);
							Concept cp = new Concept();
							cp.setId(ThsSpider.START_THS + code);
							cp.setCode(code);
							cp.setHref(href);
							cp.setName(name);
							cp.setDate(date);
							cp.setAliasCode(code);
							cp.setType(ThsSpider.thsHye);
							if (isFirday) {
								cp.setCnt(getSubCodeList(urlSubBase, cp, ThsSpider.thsHye));
							}
							//
							tds.next();// 涨跌幅(%)
							tds.next();// 总成交量（万手）
							tds.next();// 总成交额（亿元）
							tds.next();// 净流入（亿元）
							tds.next();// 上涨家数
							tds.next();// 下跌家数
							tds.next();// 均价
							tds.next();// 领涨股
							tds.next();// 最新价
							//
							getConceptDaily(cp, href, date);
							// System.err.println(cp);
							// return true;
							list.add(cp);
						}
						// System.err.println(tbody.asText());
						fetched = true;
					} else {
						if (j > 2) {
							isbraek = true;
							break;
						}
					}

				} catch (Exception e2) {
					e2.printStackTrace();
				} finally {
					htmlunitSpider.close();
				}
				ThreadsUtil.sleepRandomSecBetween1And5(trytime);
				if (!fetched) {
					trytime++;
					if (trytime >= 5) {
						fetched = true;
						WxPushUtil.pushSystem1("同花顺-行业抓包出错，url=" + url);
					}
				}
			} while (!fetched);
			// ThreadsUtil.sleepRandomSecBetween1And5(trytime);
			j++;
			if (isbraek) {
				break;
			}
		}
		int c = list.size();
		saveConcept(list);
		log.info("saveConcept size:{}", c);
	}
}
