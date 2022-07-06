package com.stable.service.model.prd;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.stable.constant.Constant;
import com.stable.msg.WxPushUtil;
import com.stable.spider.realtime.RealTime;
import com.stable.spider.realtime.RealtimeCall;
import com.stable.spider.tick.TencentTick;
import com.stable.spider.tick.TencentTickHist;
import com.stable.spider.tick.TencentTickReal;
import com.stable.spider.tick.TickDay;
import com.stable.spider.tick.TickFb;
import com.stable.spider.tick.TickFz;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.vo.Prd1Monitor;
import com.stable.vo.bus.OnlineTesting;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SuppressWarnings("unused")
public class Prd1MoniWorker implements Runnable {

	private String code;
	private Prd1Monitor pm;
	private double yersterdayPrice;
	private Map<Integer, TickFz> map = new HashMap<Integer, TickFz>();
	private OnlineTesting ot;
	private int help = 9;
	public boolean stopToday = false;
	private int stpchk = 0;
	private TickService tickService;
	private boolean isException = false;
	private List<TickDay> tds = new LinkedList<TickDay>();
	private Map<String, OnlineTesting> testinglist;
	private Prd1Service prd1Service;

	public Prd1MoniWorker(Prd1Monitor p, TickService tickService, Map<String, OnlineTesting> t, Prd1Service ps) {
		this.pm = p;
		this.code = p.getCode();
		ot = p.getOnlineTesting();
		this.tickService = tickService;
		this.testinglist = t;
		if (ot != null) {
			testinglist.put(code, ot);
		}
		prd1Service = ps;
	}

	private synchronized void running() {
		try {
			// 1.初始化
			if (yersterdayPrice == 0) {
				RealTime srt = RealtimeCall.get(code);
				if (srt.getOpen() == 0.0) {
					stpchk++;
					if (stpchk >= 5) {
						stopToday = true;
					}
					log.warn("{} 疑似停牌！", code);
					return;
				}
				if (srt.getYesterday() <= 0.0) {
					return;
				}

				// 未停牌则设置昨收
				yersterdayPrice = srt.getYesterday();

				int size = 3;
				List<String> files = tickService.getLastFile(code, size);
				tds.add(TencentTickHist.readTickDayFromFile(files.get(0)));
//				tds.add(TencentTickHist.readTickDayFromFile(files.get(1)));
//				tds.add(TencentTickHist.readTickDayFromFile(files.get(2)));

				if (stpchk > 0) {// 前面的可能没有获取到完整数据
					List<TickFb> fbs = TencentTickHist.genTick(code);
					Map<Integer, TickFz> t = TencentTick.getTickFzMap(fbs, yersterdayPrice);
					for (Integer key : t.keySet()) {
						map.put(key, t.get(key));
					}
				}

			}
			// 2.获取最新交易数据
			List<TickFb> fbs = TencentTickReal.fetchRealTradesLast60(code);
			Map<Integer, TickFz> t = TencentTick.getTickFzMap(fbs, yersterdayPrice);
			for (Integer key : t.keySet()) {
				map.put(key, t.get(key));
			}
			last = fbs.get(0);
			today = TencentTick.getTickTickDay(map, true);
			boolean needBuy = false;

			// 3.根据仓位进行动态决策：买入还是卖出

//			if (ot != null) {// 是否已经买入？
//				if (help == soldPoint()) {
//					buyPoint();// 需要补仓
//				}
//			} else {
//				buyPoint();// 纯买入监听
//			}
		} catch (Exception e) {
			isException = true;
			e.printStackTrace();
			WxPushUtil.pushSystem1(code + " Prd1MoniWorker 异常！");
		}
	}

	private TickFb last = null;
	private TickDay today = null;

	public void run() {
		if (isException) {
			return;
		}
		running();
	}

	private boolean isBuy2nd = false;// 是否已经2次买入
	// 卖点:
	// 9:已套牢，需要补偿，

	// 总成本
	private double totCostPrice;
	private double tcostprice;

	// 是否获取到卖点
	private boolean soldPoint() {
		if (yersterdayPrice < last.getPrice()) {// 水上
			if (CurrencyUitl.cutProfit(totCostPrice, last.getPrice()) > 0.5) {// 至少盈利0.5%
				if (today.getAvg() > 0 && today.getUpVol() > today.getAvg() * 1.8) {// 下量超过今天的均量的2倍
					if (today.getUpVol() > (tds.get(0).getTop() * 0.9)) {// 量超过昨天最高的量的9折
						return true;
					}
				}
			}
		}
		return false;
	}

	// 是否获取到买点
	private boolean buyPoint() {
		if (yersterdayPrice > last.getPrice()) {// 水下
			if (CurrencyUitl.cutProfit(yersterdayPrice, last.getPrice()) < -1.5) {// 至少下跌-1.5%
				if (today.getAvg() > 0 && today.getDownVol() > today.getAvg() * 1.8) {// 下跌量超过今天的均量的2倍
					if (today.getDownVol() > (tds.get(0).getTop() * 0.8)) {// 量超过昨天最高的量的8折
						return true;
					}
				}
			}
		}
		return false;
	}

	private void buyAction() {
		int date = DateUtil.getTodayIntYYYYMMDD();
		RealTime srt = RealtimeCall.get(code);
		if (srt.getSell1() > 0) {
			if (ot == null) {// 第一次买
				OnlineTesting ott = new OnlineTesting();
				ott.setCode(code);
				ott.setDate(date);
				ott.setPrd(pm.getBuy().getPrd());
				ott.setPrdsub(pm.getBuy().getPrdsub());
				ott.setIdkey();
				ott.setStat(1);
				ott.setCostPrice(srt.getSell1());
				ott.setVol(getVol(ott.getCostPrice()));
				ott.setCanSold(0);
				ott.setBuyToday(ott.getVol());
				ott.setProfitAmt(0);
				ott.setProfitPct(0);
				ott.setTimes(0);
				ott.setCost1st(CurrencyUitl.multiplyDecimal(ott.getCostPrice(), ott.getVol()).doubleValue());
				String hist = getTime() + "+" + ott.getCostPrice() + "x" + ott.getVol();
				ott.setHist(hist);
				this.ot = ott;
				prd1Service.saveTesting(ott);
				testinglist.put(code, ott);
			}
		}
	}

	private String getTime() {
		return DateUtil.formatDate(new Date(), DateUtil.YYYY_MM_DD3_HHMMSS);
	}

	private void soldAction() {
		int date = DateUtil.getTodayIntYYYYMMDD();
		RealTime srt = RealtimeCall.get(code);
		if (srt.getBuy1() > 0) {
			if (ot != null && ot.getCanSold() > 0) {
				ot.setCostPrice(srt.getBuy1());
				String hist = ot.getHist() + "|" + getTime() + "-" + srt.getBuy1() + "x" + ot.getCanSold();
				ot.setHist(hist);

				if (ot.getVol() == ot.getCanSold()) {// 已买完
					ot.setStat(2);
					ot.setVol(0);
					// 计算利润
				} else {
					ot.setVol(ot.getVol() - ot.getCanSold());
					// 计算成本
				}
				ot.setCanSold(0);
				prd1Service.saveTesting(ot);
			}
		}
	}

	// 股：10万成本价10.01，一共可买9990，取整100， 9990.100 -> 99 *100 = 9900
	private int getVol(double buyPrice) {
		int vol = CurrencyUitl.divideDecimal(Constant.WAN_10, buyPrice).intValue();
		return (vol / 100) * 100;
	}

}
