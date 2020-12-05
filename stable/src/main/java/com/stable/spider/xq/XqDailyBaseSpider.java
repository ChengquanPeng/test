package com.stable.spider.xq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class XqDailyBaseSpider {
	private static final String SPLIT = "：";
	@Autowired
	private HtmlunitSpider htmlunitSpider;// = new HtmlunitSpider();
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;

	private String F1 = "市盈率(静)";
	private String F2 = "市盈率(动)";
	private String F3 = "市盈率(TTM)";
	private String F4 = "市净率";
	// https://xueqiu.com/S/SH600109
	// https://xueqiu.com/S/SZ000001
	private String BASE_URL = "https://xueqiu.com/S/%s";

	public static String formatCode2(String code) {
		// 5开头，沪市基金或权证 60开头上证
		if (code.startsWith("6")) {
			return String.format("SH%s", code);
		} else {
			return String.format("SZ%s", code);
		}
	}

	public void fetchAll(List<DaliyBasicInfo> list) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					List<DaliyBasicInfo> upd = new LinkedList<DaliyBasicInfo>();
					for (DaliyBasicInfo b : list) {
						if (dofetch(b)) {
							upd.add(b);
						}
					}
					if (upd.size() > 0) {
						esDaliyBasicInfoDao.saveAll(list);
					}
					if (upd.size() != list.size()) {
						WxPushUtil.pushSystem1("雪球=>每日指标-市盈率记录抓包不完整,期望数:{" + list.size() + "},实际成功数:" + upd.size());
					}
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("雪球=>每日指标-市盈率记录抓包出错");
				}
			}
		}).start();
	}

	private boolean dofetch(DaliyBasicInfo b) {
		String code = b.getCode();
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(BASE_URL, formatCode2(code));
		do {
			ThreadsUtil.sleepRandomSecBetween1And5();
			HtmlPage page = null;
			HtmlElement body = null;
			try {
				log.info(url);
				page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url);
				body = page.getBody();
				HtmlElement table = body.getElementsByAttribute("table", "class", "quote-info").get(0);// table
				DomElement tbody = table.getChildElements().iterator().next();// tbody
				Iterator<DomElement> trs = tbody.getChildElements().iterator();
				while (trs.hasNext()) {
					Iterator<DomElement> tds = trs.next().getChildElements().iterator();
					while (tds.hasNext()) {
						String s = tds.next().asText();
						if (s.contains(F1)) {// "市盈率(静)";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPe(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
								b.setPe(-1);
							}
						} else if (s.contains(F2)) {// "市盈率(动)";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPe_d(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
								b.setPe_d(-1);
							}
						} else if (s.contains(F3)) {// "市盈率(TTM)";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPe_ttm(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
								b.setPe_ttm(-1);
							}
						} else if (s.contains(F4)) {// "市净率";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPb(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
								b.setPb(-1);
							}
						} else {

						}
					}
				}
				if (b.getPb() != 0.0d) {
					return true;
				}
				// System.err.println(boardInfos.asText());
			} catch (Exception e2) {
				e2.printStackTrace();
			} finally {
				htmlunitSpider.close();
			}

			trytime++;
			ThreadsUtil.sleepRandomSecBetween1And5(trytime);
			if (trytime >= 10) {
				fetched = true;
				WxPushUtil.pushSystem1("雪球每日信息出错(pe,pe-ttm),code={}" + code + ",url=" + url);
			}
		} while (!fetched);
		return false;
	}

	public static void main(String[] args) {
		// XqDailyBaseSpider x = new XqDailyBaseSpider();
		// DaliyBasicInfo b = new DaliyBasicInfo();
		// System.err.println(b.getPb());
	}
}
