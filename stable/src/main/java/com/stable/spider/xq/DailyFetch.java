package com.stable.spider.xq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.service.StockBasicService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DailyFetch {
	private static final String SPLIT = "：";
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;

	// https://xueqiu.com/S/SH600109
	// https://xueqiu.com/S/SZ000001
	private String BASE_URL = "https://xueqiu.com/S/%s";
	private String SHOU = "手";
	private String BFH = "%";
	private String DOL = "¥";
	private String JIA = "+";
	private String JIAN = "-";

	private String F1 = "市盈率(静)";
	private String F2 = "市盈率(动)";
	private String F3 = "市盈率(TTM)";
	private String F4 = "市净率";
	private String F5 = "流通值";
	private String F6 = "总市值";
	private String F7 = "总股本";
	private String F8 = "流通股";

	private String D1 = "最高";
	private String D2 = "最低";
	private String D3 = "今开";
	private String D4 = "昨收";
	private String D5 = "成交量";
	private String D6 = "成交额";
	private String D7 = "换手";
	// 今日涨跌额
	// 今日涨跌幅

	public synchronized void fetchAllHushenCodes() {
		int date = DateUtil.getTodayIntYYYYMMDD();
		List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliyNofq> listNofq = new LinkedList<TradeHistInfoDaliyNofq>();
		List<DaliyBasicInfo2> daliybasicList = new LinkedList<DaliyBasicInfo2>();

		List<StockBaseInfo> codes = stockBasicService.getAllOnStatusListWithOutSort();
		for (StockBaseInfo b : codes) {
			dofetch(b.getCode(), date, list, listNofq, daliybasicList);
		}
	}

	private void dofetch(String code, int date, List<TradeHistInfoDaliy> listtd, List<TradeHistInfoDaliyNofq> listNofq,
			List<DaliyBasicInfo2> daliybasicList) {

		DaliyBasicInfo2 b = new DaliyBasicInfo2(code, date);
		b.setPe(-1);
		b.setPed(-1);
		b.setPeTtm(-1);
		b.setPb(-1);

		int trytime = 0;
		boolean fetched = false;
		String url = String.format(BASE_URL, XqDailyBaseSpider.formatCode2(code));
		do {
			ThreadsUtil.sleepRandomSecBetween1And2();
			HtmlPage page = null;
			HtmlElement body = null;
			try {
//				log.info(url);
				page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url);
				body = page.getBody();

				HtmlElement table = body.getElementsByAttribute("table", "class", "quote-info").get(0);// table
				DomElement tbody = table.getChildElements().iterator().next();// tbody
				Iterator<DomElement> trs = tbody.getChildElements().iterator();
				TradeHistInfoDaliy td = new TradeHistInfoDaliy();
				while (trs.hasNext()) {
					Iterator<DomElement> tds = trs.next().getChildElements().iterator();
					while (tds.hasNext()) {
						String s = tds.next().asText();
						if (s.contains(F1)) {// "市盈率(静)";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPe(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
							}
						} else if (s.contains(F2)) {// "市盈率(动)";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPed(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
							}
						} else if (s.contains(F3)) {// "市盈率(TTM)";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPeTtm(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
							}
						} else if (s.contains(F4)) {// "市净率";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setPb(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
							}
						} else if (s.contains(F5)) {// "流通值";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setCircMarketVal(Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "")));
							} catch (Exception e) {
								try {
									String wy = s.split(SPLIT)[1].replace(CurrencyUitl.YI, "");
									if (wy.contains(CurrencyUitl.WAN)) {
										b.setCircMarketVal(Double.valueOf(wy.replace(CurrencyUitl.WAN, "")) * 10000);
									}
								} catch (Exception e2) {

								}
							}
						} else if (s.contains(F6)) {// "总市值";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setTotalMarketVal(Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "")));
							} catch (Exception e) {
								try {
									String wy = s.split(SPLIT)[1].replace(CurrencyUitl.YI, "");
									if (wy.contains(CurrencyUitl.WAN)) {
										b.setTotalMarketVal(Double.valueOf(wy.replace(CurrencyUitl.WAN, "")) * 10000);
									}
								} catch (Exception e2) {

								}
							}
						} else if (s.contains(F7)) {// "总股本";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setTotalShare(Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "")));
							} catch (Exception e) {
								try {
									String w = s.split(SPLIT)[1].replace(CurrencyUitl.YI, "");
									if (w.contains(CurrencyUitl.WAN)) {
										b.setTotalShare(Double.valueOf(w.replace(CurrencyUitl.WAN, "")) * 10000);
									}
								} catch (Exception e2) {

								}
							}
						} else if (s.contains(F8)) {// "floatShare";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setFloatShare(Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "")));
							} catch (Exception e) {
								try {
									String w = s.split(SPLIT)[1].replace(CurrencyUitl.YI, "");
									if (w.contains(CurrencyUitl.WAN)) {
										b.setFloatShare(Double.valueOf(w.replace(CurrencyUitl.WAN, "")) * 10000);
									}
								} catch (Exception e2) {

								}
							}
						} else if (s.startsWith(D1)) {// "最高"
							try {
								td.setHigh(Double.valueOf(s.split(SPLIT)[1]));
							} catch (Exception e) {
								// 停牌标志
								List<HtmlElement> list = body.getElementsByAttribute("div", "class", "stock-flag");
								if (list != null && list.size() > 0) {
									log.info("{} 今日停牌", code);
									return;// 成功-停牌
								}
							}

						} else if (s.startsWith(D2)) {// "最低"
							td.setLow(Double.valueOf(s.split(SPLIT)[1]));

						} else if (s.startsWith(D3)) {// "今开"
							td.setOpen(Double.valueOf(s.split(SPLIT)[1]));

						} else if (s.startsWith(D4)) {// "昨收"
							td.setYesterdayPrice(Double.valueOf(s.split(SPLIT)[1]));

						} else if (s.startsWith(D5)) {// "成交量" //手
							if (s.contains(CurrencyUitl.WAN)) {
								td.setVolume(Double
										.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.WAN, "").replace(SHOU, ""))
										* 100 * 10000);
							} else if (s.contains(CurrencyUitl.YI)) {
								td.setVolume(
										Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "").replace(SHOU, ""))
												* 100 * 10000 * 10000);
							} else {// 只有手
								td.setVolume(Double.valueOf(
										s.split(SPLIT)[1].replace(CurrencyUitl.WAN, "").replace(SHOU, "")) * 100);
							}
							// System.err.println(Double.valueOf(td.getVolume()).longValue());
						} else if (s.startsWith(D6)) {// "成交额" //元
							if (s.contains(CurrencyUitl.YI)) {
								td.setAmt(
										Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "")) * 10000 * 10000);
							} else if (s.contains(CurrencyUitl.WAN)) {
								td.setAmt(Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.WAN, "")) * 10000);
							}
							// System.err.println(Double.valueOf(td.getAmt()).longValue());
						} else if (s.startsWith(D7)) {// "换手"
							td.setChangeHands(Double.valueOf(s.split(SPLIT)[1].replace(BFH, "")));
						}
					}
				}

				if (b.getPb() != 0.0d) {

					td.setQfqDate(date);
					td.setDate(date);
					td.setCode(code);
					td.setId();
					if (td.getHigh() > 0 && td.getLow() > 0 && td.getOpen() > 0) {
						// 收盘
						HtmlElement stockCurrent = body.getElementsByAttribute("div", "class", "stock-current").get(0);
						td.setClosed(Double.valueOf(stockCurrent.asText().trim().replace(DOL, "")));

						// 涨跌幅额
						HtmlElement stockChange = body.getElementsByAttribute("div", "class", "stock-change").get(0);
						String[] ss = stockChange.asText().trim().replace(JIA, "").replace(JIAN, "").replace(BFH, "")
								.split(" ");
						td.setTodayChange(Double.valueOf(ss[0]));
						td.setTodayChangeRate(Double.valueOf(ss[1]));

						b.setClosed(td.getClosed());

						listtd.add(td);
						listNofq.add(new TradeHistInfoDaliyNofq(td));
						daliybasicList.add(b);
					} else {
						MsgPushServer.pushSystem1("雪球获取日交易异常？" + code + ",url=" + url);
					}
					return;// 成功-正常
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
				MsgPushServer.pushSystem1("雪球每日信息出错(pe,pe-ttm),code=" + code + ",url=" + url);
			}
		} while (!fetched);
		return;// 失败
	}

	public static void main(String[] args) {
		DailyFetch x = new DailyFetch();
		x.htmlunitSpider = new HtmlunitSpider();
		String code = "600665";
		List<TradeHistInfoDaliy> listtd = new LinkedList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliyNofq> listNofq = new LinkedList<TradeHistInfoDaliyNofq>();
		List<DaliyBasicInfo2> daliybasicList = new LinkedList<DaliyBasicInfo2>();
		x.dofetch(code, DateUtil.getTodayIntYYYYMMDD(), listtd, listNofq, daliybasicList);
		System.err.println(listtd.get(0));
		System.err.println(listNofq.get(0));
		System.err.println(daliybasicList.get(0));
	}
}
