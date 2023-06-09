package com.stable.spider.ths;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.EsBuyBackInfoDao;
import com.stable.es.dao.base.EsReducingHoldingSharesDao;
import com.stable.service.StockBasicService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.bus.ReducingHoldingShares;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 近期重要事件
 */
@Component
@Log4j2
public class ThsEventSpider {
	private static final String YG = "亿股";
	private static final String WG = "万股";
	private static final String HG_0 = "回购";
	private static final String ZC_0 = "增持";
	private static final String JC_0 = "减持";
	private static final String JC_NO = "不减持";
	private static final String JC_GGJC = "高管减持";
	private static final String JC_PLAN = "增减持计划";
	private static final String JC_GDJC = "股东减持";

	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;
	@Autowired
	private EsReducingHoldingSharesDao reducingHoldingSharesDao;

	private String urlbase = "https://basic.10jqka.com.cn/%s/event.html#stockpage?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;
	private int endDate2 = 0;

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
			MsgPushServer.pushToSystem("同花顺-近期重要事件-异常运行异常");
		}
	}

	private synchronized void dofetchInner2() {
		int sysdate = DateUtil.getTodayIntYYYYMMDD();
		endDate2 = DateUtil.addDate(sysdate, -1000);
		if (header == null) {
			header = new HashMap<String, String>();
		}
		int pre3Year = DateUtil.getPreYear(DateUtil.getTodayIntYYYYMMDD(), 3);
		List<ReducingHoldingShares> list0 = new LinkedList<ReducingHoldingShares>();
		List<BuyBackInfo> list2 = new LinkedList<BuyBackInfo>();

		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		log.info("codelist.size:" + codelist.size());
		int c = 0;
		for (StockBaseInfo s1 : codelist) {
			try {
				String code = s1.getCode();
				boolean online3Year = stockBasicService.onlinePreYearChk(code, pre3Year);
				if (online3Year && stockBasicService.xiaoshizhi(s1)) {
					dofetchInner3(code, list0, list2);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
			c++;
			log.info("ThsEventSpider current index:{}", c);
			if (list0.size() > 200) {
				reducingHoldingSharesDao.saveAll(list0);
				list0 = new LinkedList<ReducingHoldingShares>();
			}
			if (list2.size() > 200) {
				buyBackInfoDao.saveAll(list2);
				list2 = new LinkedList<BuyBackInfo>();
			}
		}
		if (list0.size() > 0) {
			reducingHoldingSharesDao.saveAll(list0);
		}
		if (list2.size() > 0) {
			buyBackInfoDao.saveAll(list2);
		}
		log.info("同花顺-近期重要事件-done");
	}

	// 增减持
	private void dofetchInner3(String code, List<ReducingHoldingShares> list0, List<BuyBackInfo> list2) {
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(urlbase, code, System.currentTimeMillis());
		ThreadsUtil.sleepRandomSecBetween5And15Ths();
		do {
			try {
//				log.info(url);
				header.put("Referer", host + code + "/");
				HtmlPage page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url, header);
				HtmlElement body = page.getBody();
				// System.err.println(body.asText());
				HtmlElement table = body.getElementsByAttribute("table", "id", "tableList").get(0);
				DomElement tbody = table.getFirstElementChild();

				Iterator<DomElement> it0 = tbody.getChildElements().iterator();
				while (it0.hasNext()) {
					DomElement tr = it0.next();
					DomElement td1 = tr.getFirstElementChild();
					DomElement td2 = tr.getLastElementChild();
					String desc = td2.asText().trim();
					String key4 = desc;
					if (desc.length() > 4) {
						key4 = desc.substring(0, 4);
					}
//					System.err.println(key4);

					if (key4.contains(JC_0)) {// 减持：股东减持，高管减持， 增减持计划
						if (desc.contains(ZC_0)) {
							buyback(code, td1.asText(), desc, list2, 1);// 增持
						} else {
							reduce(code, td1.asText(), desc, list0);// 减持
						}
					} else if (key4.contains(ZC_0)) {
						buyback(code, td1.asText(), desc, list2, 1);// 增持
					} else if (key4.contains(HG_0)) {
						buyback(code, td1.asText(), desc, list2, 2);// 回购
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
					MsgPushServer.pushToSystem("同花顺-近期重要事件-获取出错,url=" + url);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
	}

	// 1增持&2回购
	private void buyback(String code, String td1, String desc, List<BuyBackInfo> list, int type) {
		desc = desc.replaceAll(" ", "").replaceAll("\r\n", "").replaceAll("\n", "");
		BuyBackInfo bb = new BuyBackInfo();
		bb.setCode(code);
		bb.setDate(DateUtil.convertDate2(td1.trim()));
		bb.setId(code + bb.getDate());
		int endi = desc.length();
		if (endi > 100) {
			endi = 100;
		}
		bb.setDesc(desc.substring(0, endi));
		bb.setType(type);
		if (bb.getDate() > endDate2) {
			list.add(bb);
		}
//		System.err.println(bb);
	}

	// 减持
	private void reduce(String code, String td1, String desc, List<ReducingHoldingShares> list) {
		desc = desc.replaceAll(" ", "").replaceAll("\r\n", "").replaceAll("\n", "");
		if (desc.contains(JC_NO)) {
			return;
		}
		int date1 = DateUtil.convertDate2(td1.trim());
		if (date1 < endDate2) {
			return;
		}
		ReducingHoldingShares rhs = new ReducingHoldingShares();
		rhs.setCode(code);
		rhs.setDate(date1);
		rhs.setId(code + rhs.getDate());

		int endi = desc.length();
		if (endi > 100) {
			endi = 100;
		}
		rhs.setDesc(desc.substring(0, endi));
		rhs.setType(getReduceType(desc));
		int i1 = desc.indexOf(WG);// 万股
		boolean yg = false;
		if (i1 < 0) {
			i1 = desc.indexOf(YG);// 亿股
			yg = true;
		}
		if (i1 > 0) {
			String s = desc.substring(0, i1);
			String r = "";
			for (int i = (s.length() - 1); i >= 0; i--) {
				char t = s.charAt(i);
				if ((t >= 48 && t <= 57) || (t == '.')) {
					r = t + r;
				} else {
					break;
				}
			}
			if (yg) {
				rhs.setWg(Double.valueOf(r) * 10000);
			} else {
				rhs.setWg(Double.valueOf(r));
			}
		}
		list.add(rhs);
//		System.err.println(rhs);
//		System.err.println(rhs.getType() + " " + rhs.getDate() + " " + rhs.getWg() + "万股       " + rhs.getDesc());
	}

	// 1.股东减持,2.增减持计划,3.高管减持,4.其他
	private int getReduceType(String desc) {
		if (desc.startsWith(JC_GDJC)) {
			return 1;
		}
		if (desc.startsWith(JC_PLAN)) {
			return 2;
		}
		if (desc.startsWith(JC_GGJC)) {
			return 3;
		}
		return 4;
	}

	public static void main(String[] args) {
		ThsEventSpider ts = new ThsEventSpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		ts.dofetchInner3("600400", new LinkedList<ReducingHoldingShares>(), new LinkedList<BuyBackInfo>());
	}
}
