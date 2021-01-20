package com.stable.spider.xq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.service.FinanceService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.FinanceBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class XqDailyBaseSpider {
	private static final String SPLIT = "：";
	@Autowired
	private HtmlunitSpider htmlunitSpider;// = new HtmlunitSpider();
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private TushareSpider tushareSpider;

	private String F1 = "市盈率(静)";
	private String F2 = "市盈率(动)";
	private String F3 = "市盈率(TTM)";
	private String F4 = "市净率";
	private String F5 = "流通值";
	private String F6 = "总市值";
	private String F7 = "总股本";
	private String F8 = "流通股";
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

	public void fetchAll(List<DaliyBasicInfo2> list) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String today = DateUtil.getTodayYYYYMMDD();
					List<DaliyBasicInfo2> upd = new LinkedList<DaliyBasicInfo2>();
					for (DaliyBasicInfo2 b : list) {
						if (dofetch(b, today)) {
							upd.add(b);
						}
						// 市赚率
						// 市盈率/净资产收益率（PE/ROE）
						FinanceBaseInfo fbi = financeService.getLastFinaceReport(b.getCode());
						if (fbi != null && fbi.getJqjzcsyl() != 0.0 && b.getPeTtm() > 0) {
							if (fbi.getSyldjd() != 0) {
								b.setSzl(CurrencyUitl.roundHalfUp(b.getPeTtm() / fbi.getSyldjd()));
							} else {
								double syldjd = CurrencyUitl.roundHalfUp(fbi.getJqjzcsyl() / (double) fbi.getQuarter());
								b.setSzl(CurrencyUitl.roundHalfUp(b.getPeTtm() / syldjd));
							}
						}
					}
					if (upd.size() > 0) {
						esDaliyBasicInfoDao.saveAll(list);
					}
					log.info("雪球=>每日指标-市盈率完成,期望数:{" + list.size() + "},实际成功数:" + upd.size());
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

	private boolean dofetch(DaliyBasicInfo2 b, String today) {
		b.setPe(-1);
		b.setPed(-1);
		b.setPeTtm(-1);
		b.setPb(-1);
		// TODO

		String code = b.getCode();
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(BASE_URL, formatCode2(code));
		do {
			ThreadsUtil.sleepRandomSecBetween1And2();
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
								b.setCircMarketVal(Double.valueOf(s.split(SPLIT)[1].replace("亿", "")));
							} catch (Exception e) {
							}
						} else if (s.contains(F6)) {// "总市值";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setTotalMarketVal(Double.valueOf(s.split(SPLIT)[1].replace("亿", "")));
							} catch (Exception e) {
							}
						} else if (s.contains(F7)) {// "总股本";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setTotalShare(Double.valueOf(s.split(SPLIT)[1].replace("亿", "")));
							} catch (Exception e) {
							}
						} else if (s.contains(F8)) {// "floatShare";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setFloatShare(Double.valueOf(s.split(SPLIT)[1].replace("亿", "")));
							} catch (Exception e) {
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
				WxPushUtil.pushSystem1("雪球每日信息出错(pe,pe-ttm),用tushare进行补充,code={}" + code + ",url=" + url);
				try {
					JSONArray array = tushareSpider
							.getStockDaliyBasic(TushareSpider.formatCode(code), today, null, null)
							.getJSONArray("items");
					JSONArray arr = array.getJSONArray(0);

					int i = 0;
					arr.getString(i++);// ts_code
					arr.getString(i++);
					arr.getString(i++);
					arr.getString(i++);
					arr.getString(i++);
					arr.getString(i++);

					try {
						b.setPe(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
					try {
						b.setPeTtm(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
					try {
						b.setPb(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
					arr.getString(i++);
					arr.getString(i++);
					arr.getString(i++);
					arr.getString(i++);
					try {
						b.setTotalShare(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
					try {
						b.setFloatShare(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
					try {
						b.setTotalMarketVal(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
					try {
						b.setCircMarketVal(Double.valueOf(arr.getString(i++)));
					} catch (Exception e) {
					}
				} catch (Exception e) {
				}
			}
		} while (!fetched);
		return false;

	}

	public static void main(String[] args) {
		XqDailyBaseSpider x = new XqDailyBaseSpider();
		x.htmlunitSpider = new HtmlunitSpider();
		DaliyBasicInfo2 b = new DaliyBasicInfo2();
		b.setCode("300519");
		System.err.println(x.dofetch(b, DateUtil.getTodayYYYYMMDD()));
		System.err.println(b);
	}
}
