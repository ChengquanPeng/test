package com.stable.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@Component
public class ThsSpider {

	@Autowired
	private HtmlunitSpider htmlunitSpider;

	// private static final String FIN_RPT_URL =
	// "http://basic.10jqka.com.cn/%s/finance.html";

	private String GN_LIST = "http://q.10jqka.com.cn/gn/index/field/addtime/order/desc/page/%s/ajax/1/";
	private int ths = 1;
	private String START_THS = "THS";
	private String SPIT = "/";

	private String host = "http://127.0.0.1:8081";
//	private String host = "http://106.52.95.147:9999";
	private String url0 = host + "/web/concept/allConcepts";
	private String url1 = host + "/web/concept/addConcept";
	private String url2 = host + "/web/concept/addCodeConcept";

	private void saveConcept(List<Concept> list) {
		if (list.size() > 0) {
			ConceptAddReq req = new ConceptAddReq();
			req.setList(list);
			System.err.println(HttpUtil.doPost(url1, JSON.toJSONString(req)));
			list.clear();
		}
	}

	private void saveCodeConcept(List<CodeConcept> list) {
		if (list.size() > 0) {
			CodeConceptAddReq req = new CodeConceptAddReq();
			req.setList(list);
			System.err.println(HttpUtil.doPost(url2, JSON.toJSONString(req)));
			list.clear();
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Concept> getAllAliasCode() {
		Map<String, Concept> m = new HashMap<String, Concept>();
		JSONObject jsonObj = HttpUtil.doPost(url0);
		if (jsonObj != null) {
			JSONObject jm = jsonObj.getJSONObject("result");
			if (jm != null) {
				m = jm.toJavaObject(Map.class);
				// System.err.println(m);
			}
		}
		System.err.println(m.size());
		return m;
	}

	@PostConstruct
	private void start() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				synchGnAndCode();
			}
		}).start();
	}

	private void synchConceptDaliy() {
		Map<String, Concept> m = this.getAllAliasCode();
		
	}

	private void synchGnAndCode() {
		Date today = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(today);
		int weekday = c.get(Calendar.DAY_OF_WEEK);
		if (weekday != 6) {
			System.err.println("今日非周五");
//			return;
		}
		try {
			List<Concept> list = new LinkedList<Concept>();
			List<CodeConcept> codelist = new LinkedList<CodeConcept>();
			Map<String, Concept> map = getAllAliasCode();
			getGnList(list, codelist, map);
			if (list.size() > 0) {
				saveConcept(list);
			}
			if (codelist.size() > 0) {
				saveCodeConcept(codelist);
			}
			WxPushUtil.pushSystem1("同花顺板块同步成功");
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("同花顺板块出错");
		}
	}

	public void getGnList(List<Concept> list, List<CodeConcept> codelist, Map<String, Concept> map) {
		int index = 1;// TODO
		int end = 0;
		int trytime = 0;
		do {
			String url = String.format(GN_LIST, index);
			DomElement table = null;
			HtmlPage page = null;
			try {
//				header.put(REFERER, refer);
				page = htmlunitSpider.getHtmlPageFromUrl(url);
				table = page.getBody().getFirstElementChild();
			} catch (Exception e) {
				e.printStackTrace();
				WxPushUtil.pushSystem1("同花顺概念-列表抓包出错,url=" + url);
				throw new RuntimeException(e);
			} finally {
				ThreadsUtil.sleepRandomSecBetween5And15();
			}
			try {
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
					list.add(cp);
					getAliasCdoe(cp, map);
					System.err.println(cp);
					getSubCodeList(cp, codelist);
					if (codelist.size() > 100) {
						saveCodeConcept(codelist);
					}
					if (list.size() >= 100) {
						saveConcept(list);
					}
				}
				index++;
				if (end == 0) {
					String pageInfo = page.getElementById("m-page").getLastElementChild().asText();
					end = Integer.valueOf(pageInfo.split(SPIT)[1]);
				}
				trytime = 0;
			} catch (Exception e) {
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("同花顺概念-列表抓包出错,url=" + url);
					if (list.size() > 0) {
						saveConcept(list);
					}
					if (codelist.size() > 0) {
						saveCodeConcept(codelist);
					}
					throw new RuntimeException(e);
				}

			}
		} while (index <= end);
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
			if (StringUtils.isNotBlank(aliasCode) && !"null".equals(aliasCode)) {
				cp.setAliasCode(aliasCode);
				map.put(cp.getCode(), cp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("同花顺概念-成分股抓包出错,url=" + cp.getHref());
			throw new RuntimeException(e);
		}
	}

	private void getSubCodeList(Concept cp, List<CodeConcept> codelist) {
		int updateTime = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		int index = 1;
		int end = 0;
		int trytime = 0;
		do {
			ThreadsUtil.sleepRandomSecBetween5And15();

			String url = String.format(GN_CODE_LIST, index, cp.getCode());
			System.err.println(url);
			DomElement table = null;
			HtmlPage page = null;
			try {
//				header.put(REFERER, cp.getHref());
				page = htmlunitSpider.getHtmlPageFromUrl(url);
				table = page.getBody().getFirstElementChild();
			} catch (Exception e) {
				e.printStackTrace();
				WxPushUtil.pushSystem1("同花顺概念-成分股抓包出错,url=" + url);
				throw new RuntimeException(e);
			}

			try {
				DomElement tbody = table.getLastElementChild();
				if (tbody.asText().contains("暂无成份股数据")) {
					return;
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
					cc.setType(ths);
					cc.setId(cp.getId() + cc.getCode());
					codelist.add(cc);
					System.err.println(cc);
				}
				index++;
				if (cp.getCnt() > 10 && end == 0) {
					String pageInfo = page.getElementById("m-page").getLastElementChild().asText();
					end = Integer.valueOf(pageInfo.split(SPIT)[1]);
				}
				trytime = 0;
			} catch (Exception e) {
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					e.printStackTrace();
					System.err.println(page.asText());
					WxPushUtil.pushSystem1("同花顺概念-成分股抓包出错,url=" + url);
					throw new RuntimeException(e);
				}
			}
		} while (index <= end);
	}
}
