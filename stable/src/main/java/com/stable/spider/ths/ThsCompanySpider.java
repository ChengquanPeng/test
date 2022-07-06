package com.stable.spider.ths;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.msg.WxPushUtil;
import com.stable.service.StockBasicService;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 公司资料
 */
@Component
@Log4j2
public class ThsCompanySpider {
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	private String urlbase = "https://basic.10jqka.com.cn/mobile/%s/profilen.html#jumpaction=iframe?t=%s";
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

	private String f1 = "国有";
	private String f2 = "中央";

	private synchronized void dofetchInner2() {
		if (header == null) {
			header = new HashMap<String, String>();
		}
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		int c = 0;
		for (StockBaseInfo s : codelist) {
			try {
				FetchRes resl = dofetchInner3(s.getCode());
				// 控股股东
				String h = resl.getHolder();
				s.setHolderName(h);
				// 最终控制人
				String r1 = resl.getFindHolder();
				s.setFinalControl(r1.trim());
				// 企业性质
				String r = resl.getComType();
				if (r.contains(f1) || r.contains(f2)) {
					s.setCompnayType(1);
				} else {
					s.setCompnayType(0);
				}
				stockBasicService.synBaseStockInfo(s, true);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
			c++;
			log.info("current index:{}", c);
		}
		log.info("同花顺公司资料 done");
	}

	class FetchRes {
		String holder = "";
		String findHolder = "无控制人";
		String comType = "";

		public String getHolder() {
			return holder;
		}

		public void setHolder(String holder) {
			this.holder = holder;
		}

		public String getFindHolder() {
			return findHolder;
		}

		public void setFindHolder(String findHolder) {
			this.findHolder = findHolder;
		}

		public String getComType() {
			return comType;
		}

		public void setComType(String comType) {
			this.comType = comType;
		}
	}

	private FetchRes dofetchInner3(String code) {
		FetchRes fr = new FetchRes();
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
				HtmlElement divt = body.getElementsByAttribute("div", "id", "detailData").get(0);
				HtmlElement table = divt.getElementsByAttribute("table", "class", "leveldatail-tab").get(0);
				DomElement tbody = table.getFirstElementChild();
				Iterator<DomElement> it0 = tbody.getChildElements().iterator();
				it0.next();// 公司名称
				it0.next();// 曾用名
				it0.next();// 注册地址
				it0.next();// 董事长
				it0.next();// 董秘
				it0.next();// 主营业务
				DomElement cot = it0.next();// 经营性质
				try {
					DomElement td = cot.getLastElementChild();
					fr.setComType(td.asText().trim());
				} catch (Exception e3) {
					e3.printStackTrace();
				}
				DomElement holder = it0.next();// 控股股东
				DomElement realHolder = it0.next();// 实际控制人
				DomElement finalHolder = it0.next();// 最终控制人
				if (finalHolder.getAttribute("id").contains("mng")) {// 实际是:实际控制人的的持股一览表
					finalHolder = it0.next();// 最终控制人
				}

				try {
					try {
						DomElement td1 = holder.getLastElementChild();
						fr.setHolder(td1.asText().trim());
					} catch (Exception e) {
						e.printStackTrace();
					}
					DomElement td2 = finalHolder.getLastElementChild();
					fr.setFindHolder(td2.asText().trim());
					return fr;
				} catch (Exception e) {
					e.printStackTrace();
					try {
						DomElement td = realHolder.getLastElementChild();
						fr.setFindHolder(td.asText().trim());
						return fr;
					} catch (Exception e2) {
						e2.printStackTrace();
						return fr;
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
		return fr;
	}

	public static void main(String[] args) {
		ThsCompanySpider ts = new ThsCompanySpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		FetchRes fr = ts.dofetchInner3("601518");
		System.err.println("getComType:" + fr.getComType());
		System.err.println("getHolder:" + fr.getHolder());
		System.err.println("getFindHolder:" + fr.getFindHolder());

	}
}
