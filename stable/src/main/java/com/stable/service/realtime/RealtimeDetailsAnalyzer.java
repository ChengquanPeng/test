package com.stable.service.realtime;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.BuyModelType;
import com.stable.enums.StockAType;
import com.stable.enums.TradeType;
import com.stable.es.dao.base.MonitoringDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.model.CodeModelService;
import com.stable.service.model.UpModelLineService;
import com.stable.service.model.data.AvgService;
import com.stable.service.trace.BuyTraceService;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.Monitoring;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsAnalyzer implements Runnable {
	private long ONE_MIN = 1 * 60 * 1000;// 5MIN
	private long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private long WAIT_MIN = FIVE_MIN;
	private String code;
	private String codeName;
	private boolean isRunning = true;
	private DaliyBasicInfo ytdBasic;
	private BuyTraceService buyTraceService;
	private boolean waitingBuy = true;
	private double topPrice;
	private List<BuyTrace> buyTraces = new LinkedList<BuyTrace>();
	private List<Monitoring> monitorList = new LinkedList<Monitoring>();
	private String today = DateUtil.getTodayYYYYMMDD();
	private int itoday = Integer.valueOf(today);
	private boolean needMoniSell = false;
	private boolean needMoniBuy = false;
	private BuyTrace buyed;
	private BuyTrace selled;
	private CodeBaseModel codeBaseModel;
	RealtimeDetailsResulter resulter;
	MonitoringDao monitoringDao;

	public void stop() {
		isRunning = false;
	}

	public int init(String code, RealtimeDetailsResulter resulter, DaliyBasicHistroyService daliyBasicHistroyService,
			AvgService avgService, String codeName, BuyTraceService buyTraceService,
			DaliyTradeHistroyService daliyTradeHistroyService, CodeModelService codeModelService,
			UpModelLineService upModelLineService, MonitoringDao monitoringDao) {
		this.code = code;
		this.codeName = codeName;
		this.resulter = resulter;
		this.buyTraceService = buyTraceService;
		this.monitoringDao = monitoringDao;

		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt.getOpen() == 0.0) {
			log.info("{} {} SINA 今日停牌", code, codeName);
			WxPushUtil.pushSystem1(code + " " + codeName + "今日停牌");
			return 0;
		}

		// 买入
		List<Monitoring> list = upModelLineService.getListByCodeForList(code);
		if (list != null && list.size() > 0) {
			needMoniBuy = true;
			monitorList.addAll(list);
		}

		// 卖出
		List<BuyTrace> bts = buyTraceService.getListByCode(code, 0, TradeType.BOUGHT.getCode(),
				BuyModelType.B2.getCode(), 0, EsQueryPageUtil.queryPage9999);
		if (bts != null && bts.size() > 0) {
			buyTraces.addAll(bts);
			needMoniSell = true;
		}

		// 初始化
		ytdBasic = daliyBasicHistroyService.queryLastest(code);
		codeBaseModel = codeModelService.getLastOneByCode(code);
		getTopPrice();

		return 1;
	}

	public int getSellCnt() {
		return needMoniSell ? 1 : 0;
	}

	public int getBuyCnt() {
		return needMoniBuy ? 1 : 0;
	}

	private double getTopPrice() {
		double yesterdayPrice = ytdBasic.getClose();
		if (StockAType.isTop20(code, itoday)) {// 科创板20%涨跌幅
			topPrice = CurrencyUitl.topPrice20(yesterdayPrice);
		} else {
			boolean isST = codeName.contains("ST");
			topPrice = CurrencyUitl.topPrice(yesterdayPrice, isST);
		}
		return topPrice;
	}

	private void autoBuy(int ver, int subVer, double buyPrice) {
		if (waitingBuy && !needMoniSell) {// 已买入就不需要再买
			log.info(code + codeName + ",buyPrice[" + buyPrice + "],涨停价格topPrice[" + topPrice + "]");
			if (buyPrice > 0.0 && buyPrice < topPrice) {
				BuyTrace bt = new BuyTrace();
				bt.setBuyDate(Integer.valueOf(today));
				bt.setBuyModelType(BuyModelType.B2.getCode());
				bt.setBuyPrice(buyPrice);
				bt.setCode(code);
				bt.setVer(ver);
				bt.setSubVer(subVer);
				bt.setId();
				bt.setStatus(TradeType.BOUGHT.getCode());
				buyTraceService.addToTrace(bt);
				log.info("机器买已成交:{}", bt);
				waitingBuy = false;
				buyed = bt;
			}
		}
	}

	public void run() {
		// 买入
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

				SinaRealTime srt = SinaRealtimeUitl.get(code);
				// 买入
				if (needMoniBuy) {
					for (Monitoring m : monitorList) {
						int ver = m.getVer();
						if (ver == 1) {// 短线
							int subVer = 1;
//							if("v1") { if("v2") {//TODO
							gotBuyBiz(ver, subVer, srt, now, d1450);
						}
					}
				}

				// 卖出
//				if (needMoniSell && (now > d1450)) {// 14:50 //TODO
//					if (buyTraces.size() > 0) {
//						// 止损卖出//最高点回调5%卖
//						List<BuyTrace> sells2 = new LinkedList<BuyTrace>();
//						for (int i = 0; i < buyTraces.size(); i++) {
//
//						}
//						if (sells2.size() > 0) {
//							buyTraces.removeAll(sells2);
//						}
//					}
//				}
				if (now > d1450) {
					WAIT_MIN = ONE_MIN;
				}
				Thread.sleep(WAIT_MIN);
			} catch (Exception e) {
				if (!isPushedException) {
					WxPushUtil.pushSystem1(code + " 监听异常！");
					isPushedException = true;
					e.printStackTrace();
				}
				try {
					Thread.sleep(FIVE_MIN);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		// 监听完成，修改监听信息：
		// 1.监听买入:监听状态
		// 2.已买入-卖出:持有天数等待，是否完成卖出
		long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
		if ((now > d1450)) {
			finTodayMoni();
		}
	}

	private void gotBuyBiz(int ver, int subVer, SinaRealTime srt, long now, long d1450) {
		RealtimeMsg rm = new RealtimeMsg();
		rm.setCode(code);
		rm.setCodeName(codeName);
		rm.setBaseScore(codeBaseModel.getScore());
		rm.addMessage(getModelVerName(ver, subVer));
		resulter.addBuyMessage(code, rm);
		log.info(rm);
		if (now > d1450) {
			autoBuy(ver, subVer, srt.getSell1());
		}
	}

	private String getModelVerName(int ver, int subVer) {
		if (1 == ver) {
			String s0 = "短线";
			if (1 == subVer) {
				return s0 + "-V1";
			}
		}
		return "";
	}

	private boolean isPushedException = false;

	public BuyTrace getBuyed() {
		return buyed;
	}

	public BuyTrace getSelled() {
		return selled; // TODO
	}

	private void finTodayMoni() {
		// 修改监听状态
//		monitoringDao.saveAll(bList);
		// TODO
	}

}
