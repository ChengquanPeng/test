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
import com.stable.es.dao.base.EsStockBaseInfoDao;
import com.stable.service.StockBasicService;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 公司资料
 */
@Component
@Log4j2
public class ThsCompanySpider {
	@Autowired
	private EsStockBaseInfoDao esStockBaseInfoDao;
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	private String urlbase = "http://basic.10jqka.com.cn/%s/company.html?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;

	public void byJob() {
		dofetchInner();
	}

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
			ErrorLogFileUitl.writeError(e, "同花顺公司资料异常运行异常..", "", "");
			WxPushUtil.pushSystem1("同花顺公司资料异常运行异常");
		}
	}

	private String f1 = "国有资产";
	private String f2 = "教育部";
	private String f3 = "财政局";
	private String f4 = "财政厅";
	private String f5 = "财政部";

	private synchronized void dofetchInner2() {
		if (header == null) {
			header = new HashMap<String, String>();
		}
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		List<StockBaseInfo> upd = new LinkedList<StockBaseInfo>();
		int c = 0;
		for (StockBaseInfo s : codelist) {
			try {
				String r = dofetchInner3(s.getCode());
				if (r != null) {
					s.setFinalControl(r.trim());
					if (r.contains(f1) || r.contains(f2) || r.contains(f3) || r.contains(f4) || r.contains(f5)) {
						s.setCompnayType(1);
					} else {
						s.setCompnayType(0);
					}
					upd.add(s);
					stockBasicService.synBaseStockInfo(s, true);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
			c++;
			log.info("current index:{}", c);
		}
		if (upd.size() > 0) {
			esStockBaseInfoDao.saveAll(upd);
		}
		log.info("同花顺公司资料 done");
	}

	private String dofetchInner3(String code) {
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
				HtmlElement table = body.getElementsByAttribute("table", "class", "m_table ggintro managelist").get(0);
				DomElement tbody = table.getFirstElementChild();
				Iterator<DomElement> it0 = tbody.getChildElements().iterator();
				it0.next();// 主营业务
				it0.next();// 产品名称
				DomElement holder = it0.next();// 控股股东
				DomElement finalHolder = it0.next();// 实际控制人

				try {
					DomElement finaletr = it0.next();// 最终控制人
					DomElement td = finaletr.getFirstElementChild();
					DomElement div = td.getFirstElementChild();
					Iterator<DomElement> it1 = div.getChildElements().iterator();
					it1.next();
					it1.next();
					it1.next();
					DomElement finale = it1.next();
					String res = finale.asText();
//				System.err.println(res);
					return res;
				} catch (Exception e) {
					try {
						DomElement td = finalHolder.getFirstElementChild();
						DomElement div = td.getFirstElementChild();
						Iterator<DomElement> it1 = div.getChildElements().iterator();
						it1.next();
						return it1.next().asText();
					} catch (Exception e2) {

						try {
							DomElement td = holder.getFirstElementChild();
							DomElement div = td.getFirstElementChild();
							Iterator<DomElement> it1 = div.getChildElements().iterator();
							it1.next();
							return it1.next().asText();
						} catch (Exception e3) {
							return "无控制人";
						}
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					fetched = true;
					e2.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-公司资料获取出错,url=" + url);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
		return "";
	}

	public static void main(String[] args) {
		ThsCompanySpider ts = new ThsCompanySpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		System.err.println(ts.dofetchInner3("000002"));
	}
}
