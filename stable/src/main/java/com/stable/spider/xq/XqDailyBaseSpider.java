package com.stable.spider.xq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.DataChangeService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.CodeModelKLineService;
import com.stable.service.model.prd.PreSelectSave;
import com.stable.service.model.prd.PreSelectSearch;
import com.stable.service.model.prd.PreSelectTask;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.TasksWorkerPrd1;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.StockBaseInfo;

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
	private StockBasicService stockBasicService;
	@Autowired
	private DataChangeService dataChangeService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private PreSelectSave preSelectSave;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private CodeModelKLineService codeModelKLineService;

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
		} else if (code.startsWith("0") || code.startsWith("3")) {
			return String.format("SZ%s", code);
		} else if (code.startsWith("8") || code.startsWith("4")) {
			return String.format("BJ%s", code);
		}
		return code;

	}

	public void fetchAll(List<DaliyBasicInfo2> list) {
		new Thread(new Runnable() {
			public void run() {
				dofetchEntry(list);
			}
		}).start();
	}

	private synchronized void dofetchEntry(List<DaliyBasicInfo2> list) {
		try {
			PreSelectSearch pd1 = new PreSelectSearch(daliyTradeHistroyService, preSelectSave);
			String today = DateUtil.getTodayYYYYMMDD();
			List<DaliyBasicInfo2> upd = new LinkedList<DaliyBasicInfo2>();
			int date = DateUtil.getTodayIntYYYYMMDD();
			int s = list.size();
			for (DaliyBasicInfo2 b : list) {
				if (stockBasicService.isHuShenCode(b.getCode())) {
					try {
						if (dofetch(b, today)) {
							upd.add(b);
							// 产品1：选股程序
							TasksWorkerPrd1.add(new PreSelectTask(pd1, b.getCode(), b.getCircMarketVal(), date));
						}
						// 流通股份
						if (b.getFloatShare() > 0 && b.getTotalShare() > 0) {
							StockBaseInfo base = stockBasicService.getCode(b.getCode());
							base.setFloatShare(b.getFloatShare());
							base.setTotalShare(b.getTotalShare());
							stockBasicService.synBaseStockInfo(base, true);
						}
					} catch (Exception e) {
						e.printStackTrace();
						WxPushUtil.pushSystem1("雪球=>每日指标-市盈率记录抓包出错,code=" + b.getCode());
					}
				} else {
					s--;
				}
			}
			if (upd.size() > 0) {
				esDaliyBasicInfoDao.saveAll(list);
				dataChangeService.putPeTtmData(upd);
			}
			new Thread(new Runnable() {
				public void run() {
					pd1.done();
				}
			}).start();
			log.info("雪球=>每日指标-市盈率完成,期望数:{" + s + "},实际成功数:" + upd.size());
			if (upd.size() != s) {
				WxPushUtil.pushSystem1("雪球=>每日指标-市盈率记录抓包不完整,期望数:{" + s + "},实际成功数:" + upd.size());
			}
			ThreadsUtil.sleepRandomSecBetween15And30();
			// K线模型
			new Thread(new Runnable() {
				public void run() {
					codeModelKLineService.runKLineModel(date);
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("雪球=>每日指标-市盈率记录抓包出错");
		}
	}

	private boolean dofetch(DaliyBasicInfo2 b, String today) {
		b.setPe(-1);
		b.setPed(-1);
		b.setPeTtm(-1);
		b.setPb(-1);

		String code = b.getCode();
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(BASE_URL, formatCode2(code));
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
							}
						} else if (s.contains(F8)) {// "floatShare";
							// System.err.println(s.split(SPLIT)[1]);
							try {
								b.setFloatShare(Double.valueOf(s.split(SPLIT)[1].replace(CurrencyUitl.YI, "")));
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
				WxPushUtil.pushSystem1("雪球每日信息出错(pe,pe-ttm),code=" + code + ",url=" + url);
			}
		} while (!fetched);
		return false;

	}

	public void rerun() throws Exception {
		PreSelectSearch pd1 = new PreSelectSearch(daliyTradeHistroyService, preSelectSave);
		int date = DateUtil.getTodayIntYYYYMMDD();
		if (!tradeCalService.isOpen(date)) {
			date = tradeCalService.getPretradeDate(date);
		}
		List<DaliyBasicInfo2> list = daliyBasicHistroyService
				.queryListByCode("", date, EsQueryPageUtil.queryPage9999, SortOrder.ASC).getContent();
		log.info("PRD1 {} 获取到数据:{}条", date, list.size());
		for (DaliyBasicInfo2 b : list) {
			if (stockBasicService.isHuShenCode(b.getCode())) {
				TasksWorkerPrd1.add(new PreSelectTask(pd1, b.getCode(), b.getCircMarketVal(), date));
			}
		}
		new Thread(new Runnable() {
			public void run() {
				pd1.done();
			}
		}).start();
	}

	public void test() throws Exception {
		new Thread(new Runnable() {
			public void run() {
				try {
					rerun();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public static void main(String[] args) {
		XqDailyBaseSpider x = new XqDailyBaseSpider();
		x.htmlunitSpider = new HtmlunitSpider();
		DaliyBasicInfo2 b = new DaliyBasicInfo2();
		b.setCode("600519");
		System.err.println(x.dofetch(b, DateUtil.getTodayYYYYMMDD()));
		System.err.println(b);
	}
}
