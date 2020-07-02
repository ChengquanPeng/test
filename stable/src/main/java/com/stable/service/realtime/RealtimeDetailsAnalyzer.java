package com.stable.service.realtime;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.enums.BuyModelType;
import com.stable.enums.StockAType;
import com.stable.enums.TradeType;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.TickDataService;
import com.stable.service.model.data.AvgService;
import com.stable.service.trace.BuyTraceService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.Monitoring;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TickData;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsAnalyzer implements Runnable {
	private static final EsQueryPageReq queryPage = new EsQueryPageReq(3);
	private long ONE_MIN = 1 * 60 * 1000;// 1MIN
	private long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private long WAIT_MIN = ONE_MIN;
	private StockAvg ytdAvg;
	private TickDataService tickDataService;
	private DaliyBasicHistroyService daliyBasicHistroyService;
	private String code;
	private String codeName;
	private int lastTradeDate;
	private boolean isRunning = true;
	private DaliyBasicInfo ytdBasic;
	private double yesterdayPrice;
	private BuyTraceService buyTraceService;
	private boolean waitingBuy = true;
	private double topPrice;
	private List<BuyTrace> buyTraces = new LinkedList<BuyTrace>();
	private double checkBackLine = 0.0;
	private String today = DateUtil.getTodayYYYYMMDD();
	private int itoday = Integer.valueOf(today);
	private String firstTimeWarning = null;
	private int warningCnt = 0;
	private boolean pg = false;
	private Monitoring mv;
	private RealtimeDetailsResulter resulter;
	private boolean needMoniSell = false;

	public void stop() {
		isRunning = false;
	}

	public boolean init(Monitoring mv, RealtimeDetailsResulter resulter,
			DaliyBasicHistroyService daliyBasicHistroyService, AvgService avgService, TickDataService tickDataService,
			String codeName, BuyTraceService buyTraceService, DaliyTradeHistroyService daliyTradeHistroyService) {
		this.resulter = resulter;
		this.tickDataService = tickDataService;
		this.code = mv.getCode();
		this.codeName = codeName;
		this.lastTradeDate = mv.getReqBuyDate();
		this.daliyBasicHistroyService = daliyBasicHistroyService;
		this.buyTraceService = buyTraceService;
		this.mv = mv;

		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt.getOpen() == 0.0) {
			log.info("{} {} SINA 今日停牌", code, codeName);
			WxPushUtil.pushSystem1(code + " " + codeName + "今日停牌");
			return false;
		}

		// 买入
		if (mv.getBuy() == 1) {
			this.ytdAvg = avgService.queryListByCodeForRealtime(mv.getCode(), mv.getReqBuyDate());
			// 初始化
			ytdBasic = daliyBasicHistroyService.queryLastest(code);
			if (ytdAvg == null || ytdBasic == null) {
				WxPushUtil.pushSystem1(
						"实时:数据不全，终止监控。ytdAvg==null?" + (ytdAvg == null) + "},ytdBasic==null?" + (ytdBasic == null));
				return false;
			}
			if (lastTradeDate != ytdBasic.getTrade_date()) {
				WxPushUtil.pushSystem1("实时:数据不准，终止监控。lastTradeDate (" + lastTradeDate + ")!= ytdBasic.getTrade_date("
						+ ytdBasic.getTrade_date() + ")");
				return false;
			}
			yesterdayPrice = ytdBasic.getClose();
			topPrice = getTopPrice();
		}

		// 卖出
		List<BuyTrace> bts = buyTraceService.getListByCode(code, TradeType.BOUGHT.getCode(), BuyModelType.B2.getCode(),
				MonitoringService.querypage);
		if (bts != null && bts.size() > 0) {
			bts.forEach(x -> {
				buyTraces.add(x);
			});

			int minDate = 20991231;
			for (int i = 0; i < buyTraces.size(); i++) {
				BuyTrace bt = buyTraces.get(i);
				if (minDate > bt.getBuyDate() && bt.getBuyDate() != itoday) {
					minDate = bt.getBuyDate();
				}
			}
			if (minDate != 20991231) {
				List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCode(code, minDate, 0,
						MonitoringService.querypage, SortOrder.ASC);
				log.info("list is null?{},code={},minDate={}", (list == null), code, minDate);
				double highPriceFromBuy = list.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
						.get().getHigh();
				if (highPriceFromBuy > 0.0) {
					checkBackLine = CurrencyUitl.lowestPrice(highPriceFromBuy, true);
				}
			}
			needMoniSell = true;
		}

		return true;
	}

	public int getSellCnt() {
		return needMoniSell ? 1 : 0;
	}

	private double getTopPrice() {
		double topPrice = 0.0;
		if (StockAType.KCB == StockAType.formatCode(code)) {// 科创板20%涨跌幅
			topPrice = CurrencyUitl.topPrice20(yesterdayPrice);
		} else {
			boolean isST = codeName.contains("ST");
			topPrice = CurrencyUitl.topPrice(yesterdayPrice, isST);
		}
		return topPrice;
	}

	private void autoBuy(double buyPrice, boolean pg) {
		if (waitingBuy) {
			log.info(code + codeName + ",buyPrice[" + buyPrice + "],涨停价格topPrice[" + topPrice + "]");
			if (buyPrice > 0.0 && buyPrice < topPrice) {
				BuyTrace bt = new BuyTrace();
				bt.setBuyDate(Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
				bt.setBuyModelType(BuyModelType.B2.getCode());
				bt.setBuyPrice(buyPrice);
				bt.setCode(code);
				bt.setId();
				bt.setStatus(TradeType.BOUGHT.getCode());
				int program = pg ? 1 : 2;
				bt.setProgram(program);
				buyTraceService.addToTrace(bt);
				log.info("机器买已成交:{}" + bt);
				buyTraces.add(bt);
				waitingBuy = false;
			}
		}
	}

	public void run() {
		// 买入
		double chkPrice = 0.0;
		if (mv.getBuy() == 1) {
			List<DaliyBasicInfo> list3 = daliyBasicHistroyService
					.queryListByCodeForModel(code, lastTradeDate, queryPage).getContent();

			// 监控价-1一阳N线价
			double p1 = Arrays
					.asList(ytdAvg.getAvgPriceIndex3(), ytdAvg.getAvgPriceIndex5(), ytdAvg.getAvgPriceIndex10(),
							ytdAvg.getAvgPriceIndex20(), ytdAvg.getAvgPriceIndex30())
					.stream().max(Double::compare).get();
			// 监控价-2最少3%
			double p2 = CurrencyUitl.topPrice3p(yesterdayPrice);
			// 监控价-3大于前三天最高价
			double p3 = Arrays.asList(ytdBasic.getHigh(), list3.get(1).getHigh(), list3.get(2).getHigh()).stream()
					.max(Double::compare).get();
			// 最终监控价
			chkPrice = Arrays.asList(p1, p2, p3).stream().max(Double::compare).get();
			log.info("{}=>p1一阳N线价:{},最少3%:{},p3大于前三天最高价:{},最终监控价:{},昨收:{},今日涨停价:{}", code, p1, p2, p3, chkPrice,
					yesterdayPrice, topPrice);
			// 量控量
		}

		long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		Date date = DateUtil.parseDate(today + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(date);
		long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));

		// 卖出

		while (isRunning) {
			try {
				long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
				if (d1130 <= now && now <= d1300) {
					long from3 = new Date().getTime();
					int millis = (int) ((date.getTime() - from3));
					log.info("{},中场休息。", code);
					if (millis > 0) {
						Thread.sleep(millis);
					}
				}

				double highPrice = 0.0;
				double nowPrice = 0.0;
				SinaRealTime srt = SinaRealtimeUitl.get(code);
				highPrice = srt.getHigh();
				nowPrice = srt.getNow();
				WAIT_MIN = ONE_MIN;// 新浪1分钟频率
				log.info("{} SINA 实时:highPrice:{},nowPrice:{},监控价:{}", code, highPrice, nowPrice, chkPrice);
//				else {
//					// 切换到东方财富
//					allTickData = EastmoneySpider.getRealtimeTick(code);
//					if (allTickData != null) {
//						highPrice = allTickData.stream().max(Comparator.comparingDouble(TickData::getPrice)).get()
//								.getPrice();
//						nowPrice = allTickData.get(allTickData.size() - 1).getPrice();
//						log.info("{} 东方财富 实时:highPrice:{},nowPrice:{},监控价:{}", code, highPrice, nowPrice, chkPrice);
//					} else {
//						allTickData = Collections.emptyList();
//						log.info("{} 东方财富 实时未获取到分笔", code);
//					}
//					WAIT_MIN = FIVE_MIN;// 东方财富5分钟频率
//					isSina = false;
//				}

				// 买入
				if (mv.getBuy() == 1) {
					if (highPrice >= chkPrice && nowPrice >= chkPrice) {// 一阳穿N，并涨幅3%以上
						WAIT_MIN = FIVE_MIN;// 东方财富5分钟频率
						List<TickData> allTickData = EastmoneySpider.getRealtimeTick(code);
						// 需要看量，开高低走，上影线情况
						TickDataBuySellInfo d = tickDataService.sumTickData2(code, 0, yesterdayPrice,
								ytdBasic.getCirc_mv(), allTickData, false);
						if (!pg) {
							if (d.getProgramRate() > 0) {
								pg = true;
							} else {
								pg = tickDataService.hasProgram(code);
							}
						}
						if (firstTimeWarning == null) {
							firstTimeWarning = DateUtil.getTodayYYYYMMDDHHMMSS();
						}
						warningCnt++;

						boolean buytime = d.getBuyTimes() > d.getSellTimes();
						String msg = "关注:" + code + " " + codeName + ",市场行为:" + (buytime ? "买入" : "卖出") + ",主力行为:"
								+ (pg ? "Yes" : "No") + ",买入额:" + CurrencyUitl.covertToString(d.getBuyTotalAmt())
								+ ",卖出额:" + CurrencyUitl.covertToString(d.getSellTotalAmt()) + ",总交易额:"
								+ CurrencyUitl.covertToString(d.getTotalAmt()) + ",第一次提醒时间:" + firstTimeWarning
								+ ",提醒次数:" + warningCnt + ",chkPrice:" + chkPrice + ",当前价格:" + nowPrice;
						resulter.addBuyMessage(msg);
						log.info(msg);
						autoBuy(srt.getSell1(), pg);
					}
				}

				// 卖出
				if (buyTraces.size() > 0) {
					List<BuyTrace> sells2 = new LinkedList<BuyTrace>();
					for (int i = 0; i < buyTraces.size(); i++) {
						BuyTrace bt = buyTraces.get(i);
						//// 下跌卖出 1.非当天 2.买1大于0 3,当前价格低于5日线
						if (itoday > bt.getBuyDate() && srt.getBuy1() > 0.0 && ytdAvg.getAvgPriceIndex5() > nowPrice) {
							if (bt.getBuyPrice() >= nowPrice) {// 套牢卖出:当前价格低于买入价（有可能除权的情况）
								bt.setSoldDate(itoday);
								bt.setSoldPrice(srt.getBuy1());
								bt.setStatus(TradeType.SOLD.getCode());
								bt.setProfit(CurrencyUitl.cutProfit(bt.getBuyPrice(), bt.getSoldPrice()));
								buyTraceService.addToTrace(bt);
								log.info("机器卖已成交:{}" + bt);
								sells2.add(bt);
								WxPushUtil.pushSystem1(code + " " + codeName + " [止损卖出]," + bt.getBuyDate() + "买入价格:"
										+ bt.getBuyPrice() + ",卖出价:" + bt.getSoldPrice() + "收益:" + bt.getProfit()
										+ "%");
							} else if (checkBackLine > 0.0 && checkBackLine >= nowPrice) {// 最高点回调5%卖
								bt.setSoldDate(itoday);
								bt.setSoldPrice(srt.getBuy1());
								bt.setStatus(TradeType.SOLD.getCode());
								bt.setProfit(CurrencyUitl.cutProfit(bt.getBuyPrice(), bt.getSoldPrice()));
								buyTraceService.addToTrace(bt);
								log.info("机器卖已成交:{}" + bt);
								sells2.add(bt);
								WxPushUtil.pushSystem1(code + " " + codeName + " [最高点回调5%卖]," + bt.getBuyDate()
										+ "买入价格:" + bt.getBuyPrice() + ",卖出价:" + bt.getSoldPrice() + "收益:"
										+ bt.getProfit() + "%");
							}
						}

					}
					if (sells2.size() > 0) {
						buyTraces.removeAll(sells2);
					}

					// 14:50
					if (now > d1450) {
						boolean isLowClose = isLowClosePriceToday(srt);
						if (isLowClose || isHignOpenWithLowCloseToday(srt)) {// 上影线// 高开低走
							List<BuyTrace> sells = new LinkedList<BuyTrace>();
							for (int i = 0; i < buyTraces.size(); i++) {
								BuyTrace bt = buyTraces.get(i);
								// 1.非当天
								// 2.买1大于0
								if (itoday > bt.getBuyDate() && srt.getBuy1() > 0.0) {
									bt.setSoldDate(itoday);
									bt.setSoldPrice(srt.getBuy1());
									bt.setStatus(TradeType.SOLD.getCode());
									bt.setProfit(CurrencyUitl.cutProfit(bt.getBuyPrice(), bt.getSoldPrice()));
									buyTraceService.addToTrace(bt);
									log.info("机器卖已成交:{}", bt);
									sells.add(bt);
									WxPushUtil.pushSystem1(code + " " + codeName + " [" + (isLowClose ? "上影线" : "高开低走")
											+ "]," + bt.getBuyDate() + "买入价格:" + bt.getBuyPrice() + ",卖出价:"
											+ bt.getSoldPrice() + "收益:" + bt.getProfit() + "%");
								}

								if (sells.size() > 0) {
									buyTraces.removeAll(sells);
								}
							}
						}
					}
				}

				Thread.sleep(WAIT_MIN);
			} catch (Exception e) {
				e.printStackTrace();
				WxPushUtil.pushSystem1(code + " 监听异常！");
				try {
					Thread.sleep(WAIT_MIN);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	// 排除上影线
	public boolean isLowClosePriceToday(SinaRealTime srt) {
		if (srt.getNow() > srt.getYesterday()) {
			double up = srt.getHigh() - srt.getYesterday();
			double half = up / 2;
			double mid = CurrencyUitl.roundHalfUp(half) + srt.getYesterday();
			if (mid >= srt.getNow()) {
				return true;
			}
		}
		return false;
	}

	// 排除高开低走
	public boolean isHignOpenWithLowCloseToday(SinaRealTime srt) {
		if (srt.getNow() > ytdAvg.getAvgPriceIndex5()) {
			// 不管涨跌，收盘在5日线上
			return false;
		}
		// 开盘高于昨收，收盘低于开盘
		if (srt.getOpen() > srt.getYesterday() && srt.getOpen() > srt.getNow()) {
			return true;
		}
		return false;
	}
}
