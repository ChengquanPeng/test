package com.stable.spider.xq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyDao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyNofqDao;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.RunModelService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.eastmoney.EastmoneyQfqSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.RedisUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DailyFetch {
	private static final String STOP_2 = "--";
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsTradeHistInfoDaliyDao esTradeHistInfoDaliyDao;
	@Autowired
	private EsTradeHistInfoDaliyNofqDao esTradeHistInfoDaliyNofqDao;
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;

	private static final String SPLIT = "：";
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private RunModelService runModelService;
	@Autowired
	private MonitorPoolService monitorPoolService;

	// https://xueqiu.com/S/SH600109
	// https://xueqiu.com/S/SZ000001
	private String BASE_URL = "https://xueqiu.com/S/%s";
	public static final String SHOU = "手";
	private String BFH = "%";
	private String DOL = "¥";
	private String JIA = "+";
//	private String JIAN = "-";

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

	public synchronized void fetchAllHushenCodes(String today) {
		int date = DateUtil.getTodayIntYYYYMMDD();
		int pre1Year = DateUtil.getPreYear(date);
		List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliyNofq> listNofq = new LinkedList<TradeHistInfoDaliyNofq>();
		List<DaliyBasicInfo2> daliybasicList = new LinkedList<DaliyBasicInfo2>();

		List<StockBaseInfo> codes = stockBasicService.getAllOnStatusListWithOutSort();
		CountDownLatch cnt = new CountDownLatch(codes.size());
		String preDate = tradeCalService.getPretradeDate(today);
		TradeHistInfoDaliy td = null;
		for (int k = 0; k < codes.size(); k++) {
			StockBaseInfo b = codes.get(k);
			String code = b.getCode();
			try {
				if (!stockBasicService.onlinePreYearChk(code, pre1Year)) {
					log.info("未超过一年,code:" + code);
					continue;
				}

				// 是否需要更新缺失记录
				String yyyymmdd = redisUtil.get(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
				if (StringUtils.isBlank(yyyymmdd) || (!preDate.equals(yyyymmdd) && !yyyymmdd.equals(today)
						&& Integer.valueOf(yyyymmdd) < Integer.valueOf(today))) {//
					log.info("代码code:{}重新获取记录->redis-last:{},preDate:{},today:{},index={}", code, yyyymmdd, preDate,
							today, k);
					fetchByCodeAll(code, today);
					td = dofetch(b.getCode(), date, list, listNofq, daliybasicList);
				} else {
					log.info("代码:{},不需要重新更新记录,上个交易日期 preDate:{},上次更新日期:{},最后更新日期:{},index={}", code, preDate, yyyymmdd,
							today, k);
					td = dofetch(b.getCode(), date, list, listNofq, daliybasicList);
				}
				if (td != null) {
					priceLifeService.checkAndSetPrice(td);
					redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
				}
			} catch (Exception e) {
				e.printStackTrace();
				MsgPushServer.pushToSystem("前复权qfq获取异常==>代码:" + code);
			} finally {
				cnt.countDown();
			}
		}

		try {
			if (!cnt.await(12, TimeUnit.HOURS)) {
				MsgPushServer.pushToSystem("前复权qfq获取超时异常==>日期:" + today);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (daliybasicList.size() > 0) {
			esDaliyBasicInfoDao.saveAll(daliybasicList);
		}
		if (listNofq.size() > 0) {
			esTradeHistInfoDaliyNofqDao.saveAll(listNofq);
		}
		if (list.size() > 0) {
			esTradeHistInfoDaliyDao.saveAll(list);
		}

		log.info("雪球=>每日指标-市盈率完成,实际成功数:" + list.size());

		ThreadsUtil.sleepRandomSecBetween15And30();
		// K线模型
		new Thread(new Runnable() {
			public void run() {
				runModelService.runModel(date, false);
				// 离线价格监听
				ThreadsUtil.sleepRandomSecBetween5And15();
				monitorPoolService.priceChk(listNofq, date);
				monitorPoolService.jobBuyLowVolWarning();
			}
		}).start();
	}

	private synchronized TradeHistInfoDaliy dofetch(String code, int date, List<TradeHistInfoDaliy> listtd,
			List<TradeHistInfoDaliyNofq> listNofq, List<DaliyBasicInfo2> daliybasicList) {

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
								// 有->停牌标志
								List<HtmlElement> list = body.getElementsByAttribute("div", "class", "stock-flag");
								if (list != null && list.size() > 0) {
									log.info("{} 今日停牌", code);
									return null;// 成功-停牌
								}
								// 无:停牌标志:临时停牌
								if (s.split(SPLIT)[1].trim().equals(STOP_2)) {
									HtmlElement stockChange = body
											.getElementsByAttribute("div", "class", "stock-change").get(0);
									String[] ss = stockChange.asText().trim().replace(JIA, "").replace(BFH, "")
											.split(" ");
									td.setTodayChange(Double.valueOf(ss[0]));
									td.setTodayChangeRate(Double.valueOf(ss[1]));

									if (td.getTodayChange() == 0.0 && td.getTodayChangeRate() == 0.0) {
										log.info("{} 今日停牌", code);
										return null;// 成功-停牌
									}
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

				td.setDate(date);
				td.setCode(code);
				td.setId();
				// 收盘
				HtmlElement stockCurrent = body.getElementsByAttribute("div", "class", "stock-current").get(0);
				td.setClosed(Double.valueOf(stockCurrent.asText().trim().replace(DOL, "")));
				if (td.getClosed() > 0 && td.getHigh() > 0 && td.getLow() > 0 && td.getOpen() > 0) {
					// 涨跌幅额
					HtmlElement stockChange = body.getElementsByAttribute("div", "class", "stock-change").get(0);
					String[] ss = stockChange.asText().trim().replace(JIA, "").replace(BFH, "").split(" ");
					td.setTodayChange(Double.valueOf(ss[0]));
					td.setTodayChangeRate(Double.valueOf(ss[1]));
					int qfqDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
					td.setQfqDate(qfqDate);
					b.setClosed(td.getClosed());

					listtd.add(td);
					listNofq.add(new TradeHistInfoDaliyNofq(td));
					daliybasicList.add(b);

					return td;// 成功-正常
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
				MsgPushServer.pushToSystem("雪球每日信息出错(pe,pe-ttm),code=" + code + ",url=" + url);
			}
		} while (!fetched);
		log.info("{} 失败", code);
		return null;// 失败
	}

	// 重新覆盖抓取，所有的前复权和不复权记录
	public synchronized void fetchByCodeAll(String code, String today) {
		spiderDaliyTradeHistoryInfoFromIPOCenter(code, today, 0);
		spiderDaliyTradeHistoryInfoFromIPOCenterNofq(code, 0);
	}

	public synchronized void fetchByCode(String code) {
		String today = DateUtil.getTodayYYYYMMDD();
		String preDate = tradeCalService.getPretradeDate(today);

		List<TradeHistInfoDaliy> listtd = new LinkedList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliyNofq> listNofq = new LinkedList<TradeHistInfoDaliyNofq>();
		List<DaliyBasicInfo2> daliybasicList = new LinkedList<DaliyBasicInfo2>();
		dofetch(code, Integer.valueOf(preDate), listtd, listNofq, daliybasicList);
		if (daliybasicList.size() > 0) {
			esDaliyBasicInfoDao.saveAll(daliybasicList);
		}
		if (listNofq.size() > 0) {
			esTradeHistInfoDaliyNofqDao.saveAll(listNofq);
		}
		if (listtd.size() > 0) {
			esTradeHistInfoDaliyDao.saveAll(listtd);
		}
		log.info("代码code:{}重新获取记录-> 重新获取完成", code);
	}

	// 路由，优先eastmoney
	public boolean spiderDaliyTradeHistoryInfoFromIPOCenter(String code, String today, int fortimes) {
		priceLifeService.removePriceLifeCache(code);
		if (spiderDaliyTradeHistoryInfoFromIPOEastMoney(code, fortimes)) {
			return true;
		}
		throw new RuntimeException(code + " " + today + " 日交易获取前复权错误");
	}

	// 路由，优先eastmoney
	public boolean spiderDaliyTradeHistoryInfoFromIPOCenterNofq(String code, int fortimes) {
		if (spiderDaliyTradeHistoryInfoFromIPOEastMoneyNofq(code, fortimes)) {
			return true;
		}
		throw new RuntimeException(code + " 日交易获取No复权错误");
	}

	private boolean spiderDaliyTradeHistoryInfoFromIPOEastMoney(String code, int fortimes) {
		if (fortimes >= 3) {
			log.warn("EastMoeny 超过最大次数：code：{}，fortimes：{}", code, fortimes);
			return false;
		}
		fortimes++;
		List<TradeHistInfoDaliy> list = null;
		try {
			list = EastmoneyQfqSpider.getQfq(code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null || list.size() <= 0) {
			return spiderDaliyTradeHistoryInfoFromIPOEastMoney(code, fortimes);
		} else {
			esTradeHistInfoDaliyDao.saveAll(list);
			String date = list.get(list.size() - 1).getDate() + "";
			redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, date);
		}
		return true;
	}

	private boolean spiderDaliyTradeHistoryInfoFromIPOEastMoneyNofq(String code, int fortimes) {
		if (fortimes >= 3) {
			log.warn("EastMoeny 超过最大次数：code：{}，fortimes：{}", code, fortimes);
			return false;
		}
		fortimes++;
		List<TradeHistInfoDaliyNofq> list = null;
		try {
			list = EastmoneyQfqSpider.getWithOutfq(code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null || list.size() <= 0) {
			return spiderDaliyTradeHistoryInfoFromIPOEastMoneyNofq(code, fortimes);
		} else {
			esTradeHistInfoDaliyNofqDao.saveAll(list);
		}
		return true;
	}

	public static void main(String[] args) {
		DailyFetch x = new DailyFetch();
		x.htmlunitSpider = new HtmlunitSpider();
		String code = "002113";
		List<TradeHistInfoDaliy> listtd = new LinkedList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliyNofq> listNofq = new LinkedList<TradeHistInfoDaliyNofq>();
		List<DaliyBasicInfo2> daliybasicList = new LinkedList<DaliyBasicInfo2>();
		x.dofetch(code, DateUtil.getTodayIntYYYYMMDD(), listtd, listNofq, daliybasicList);
		System.err.println(listtd.get(0));
		System.err.println(listNofq.get(0));
		System.err.println(daliybasicList.get(0));
	}
}
