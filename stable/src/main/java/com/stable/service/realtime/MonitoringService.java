package com.stable.service.realtime;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.enums.BuyModelType;
import com.stable.enums.TradeType;
import com.stable.es.dao.base.MonitoringDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.service.TradeCalService;
import com.stable.service.model.UpModelLineService;
import com.stable.service.model.data.AvgService;
import com.stable.service.trace.BuyTraceService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.Monitoring;
import com.stable.vo.bus.TickData;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.http.resp.ReportVo;
import com.stable.vo.http.resp.ViewVo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MonitoringService {
	private static final String BR = "</br>";
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private UpModelLineService upModelLineService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private TickDataService tickDataService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private BuyTraceService buyTraceService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private MonitoringDao monitoringDao;

	static final EsQueryPageReq querypage = new EsQueryPageReq(1000);

	private Map<String, RealtimeDetailsAnalyzer> map = null;

	public synchronized void startObservable() {
		String date = DateUtil.getTodayYYYYMMDD();
		int idate = Integer.valueOf(date);
		if (!tradeCalService.isOpen(idate)) {
			WxPushUtil.pushSystem1("非交易日结束监听");
			return;
		}
		long now = new Date().getTime();
		long isAlivingMillis = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 15:03:00").getTime();
		if (now > isAlivingMillis) {// 已经超时
			log.info("now > isAlivingMillis,已超时");
			return;
		}

		String observableDate = tradeCalService.getPretradeDate(date);
		try {
			log.info("observableDate:" + observableDate);
			// 所有买卖Code List
			Set<String> allCode = new HashSet<String>();
			// 获取买入监听列表
			Set<Monitoring> bList = upModelLineService.getListByCode(querypage);
			List<Monitoring> wupdate = new LinkedList<Monitoring>();// 买入列表更新
			if (bList == null) {
				bList = new HashSet<Monitoring>();
			}

			// 获取买卖监听列表
			List<BuyTrace> sList = buyTraceService.getListByCode("", 0, TradeType.BOUGHT.getCode(),
					BuyModelType.B2.getCode(), querypage);

			// 合并
			bList.forEach(x -> {
				allCode.add(x.getCode());
			});
			if (sList != null) {
				for (BuyTrace bt : sList) {
					if (!allCode.contains(bt.getCode())) {
						allCode.add(bt.getCode());
						// list add
						Monitoring m = new Monitoring();
						m.setCode(bt.getCode());
						m.setBuy(0);// 卖出
						bList.add(m);
					}
				}
			}

			List<RealtimeDetailsAnalyzer> list = new LinkedList<RealtimeDetailsAnalyzer>();
			RealtimeDetailsResulter resulter = new RealtimeDetailsResulter();

			int buytt = 0;
			int selltt = 0;
			if (bList.size() > 0) {
				// 启动监听线程
				map = new ConcurrentHashMap<String, RealtimeDetailsAnalyzer>();
				for (Monitoring x : bList) {
					log.info(x);
					RealtimeDetailsAnalyzer task = new RealtimeDetailsAnalyzer();
					if (task.init(x, resulter, daliyBasicHistroyService, avgService, tickDataService,
							stockBasicService.getCodeName(x.getCode()), buyTraceService, daliyTradeHistroyService)) {
						new Thread(task).start();
						list.add(task);
						map.put(x.getCode(), task);

						if (x.getBuy() == 1) {
							buytt++;
							wupdate.add(x);
						}
						selltt += task.getSellCnt();

					}
				}
				// 启动结果线程
				if (list.size() > 0) {
					new Thread(resulter).start();
				}
			}

			WxPushUtil.pushSystem1("交易日" + observableDate + "开始监听实时交易，监听总数:[" + bList.size() + "],实际总数[" + list.size()
					+ "],买入[" + buytt + "],卖出[" + selltt + "]");

			long from3 = new Date().getTime();
			int millis = (int) ((isAlivingMillis - from3));
			if (millis > 0) {
				Thread.sleep(millis);
			}
			List<BuyTrace> buyedList = new LinkedList<BuyTrace>();
			List<BuyTrace> selledList = new LinkedList<BuyTrace>();
			// 到点停止所有线程
			for (RealtimeDetailsAnalyzer t : list) {
				t.stop();
				if (t.getBuyed() != null) {
					buyedList.add(t.getBuyed());
				}
				if (t.getSelled() != null) {
					selledList.add(t.getSelled());
				}
			}
			resulter.stop();

			// 修改监听状态
			if (wupdate.size() > 0) {
				wupdate.forEach(x -> {
					x.setBuy(0);
					x.setLastMoniDate(idate);
				});
				monitoringDao.saveAll(wupdate);
			}
			sendEndMessaget(buyedList, selledList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			map = null;
		}
	}

	private void sendEndMessaget(List<BuyTrace> buyedList, List<BuyTrace> selledList) {
		StringBuffer sb = new StringBuffer("交易日结束监听,买入数量[" + buyedList.size() + "],卖出数量[" + selledList.size() + "]");
		sb.append(BR);
		int index = 1;
		if (buyedList.size() > 0) {
			sb.append("买入明细：==>").append(BR);
			for (BuyTrace x : buyedList) {
				sb.append("序号:").append(index).append(x.getCode()).append(" ")
						.append(stockBasicService.getCodeName(x.getCode())).append(" 买入价格:").append(x.getBuyPrice())
						.append(" 买入时间:").append(x.getBuyDate()).append(BR);
				index++;
			}
		}
		if (selledList.size() > 0) {
			sb.append("卖出明细：==>").append(BR);
			for (BuyTrace x : selledList) {
				sb.append("序号:").append(index).append(x.getCode()).append(" ")
						.append(stockBasicService.getCodeName(x.getCode())).append(" 买入时间:").append(x.getBuyDate())
						.append(" 买入价格:").append(x.getBuyPrice()).append(" 卖出价格:").append(x.getSoldPrice())
						.append(" 收益:").append(x.getProfit()).append("%").append(BR);
				index++;
			}
		}
		WxPushUtil.pushSystem2(sb.toString());
	}

	public void stopThread(String code) {
		if (map != null) {
			if (map.containsKey(code)) {
				map.get(code).stop();
			}
		}
	}

	public String todayBillingDetailReport(String code) {
		List<TickData> allTickData = EastmoneySpider.getRealtimeTick(code);
		if (allTickData == null || allTickData.isEmpty()) {
			return "没有找到今日数据";
		}
		DaliyBasicInfo ytdBasic = daliyBasicHistroyService.queryLastest(code);
		TickDataBuySellInfo d = tickDataService.sumTickData2(code, 0, ytdBasic.getClose(), ytdBasic.getCirc_mv(),
				allTickData, false);
		boolean buytime = d.getBuyTimes() > d.getSellTimes();
		boolean pg = d.getProgramRate() > 0;
		return code + " " + stockBasicService.getCodeName(code) + "==>市场行为:" + (buytime ? "买入" : "卖出") + ",主力行为:"
				+ (pg ? "Yes" : "No") + ",买入额:" + CurrencyUitl.covertToString(d.getBuyTotalAmt()) + ",卖出额:"
				+ CurrencyUitl.covertToString(d.getSellTotalAmt()) + ",总交易额:"
				+ CurrencyUitl.covertToString(d.getTotalAmt());
	}

	public String sell(String code) {
		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt != null && srt.getSell1() > 0.0) {
			List<BuyTrace> list = buyTraceService.getListByCode(code, 0, TradeType.BOUGHT.getCode(),
					BuyModelType.B1.getCode(), querypage);
			if (list != null) {
				for (BuyTrace bt : list) {
					bt.setSoldDate(Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
					bt.setSoldPrice(srt.getSell1());
					bt.setBuyModelType(TradeType.SOLD.getCode());
					buyTraceService.addToTrace(bt);
					log.info("人工卖已成交:{}" + bt);
				}
				return "成交笔数:" + list.size();
			}
		}
		return "成交笔数:0";
	}

	public BuyTrace buy(String code) {
		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt != null && srt.getBuy1() > 0.0) {
			BuyTrace bt = new BuyTrace();
			bt.setBuyDate(Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
			bt.setBuyModelType(BuyModelType.B1.getCode());
			bt.setBuyPrice(srt.getBuy1());
			bt.setCode(code);
			bt.setId();
			bt.setStatus(TradeType.BOUGHT.getCode());
			bt.setProgram(tickDataService.hasProgram(code) ? 1 : 2);
			buyTraceService.addToTrace(bt);
			log.info("人工买已成交:{}" + bt);
			return bt;
		}
		return null;
	}

	public List<ViewVo> getVeiw(String all, String fromDate) {
		int status = 0;
		if (StringUtils.isBlank(all) || "0".equals(all)) {
			status = TradeType.BOUGHT.getCode();
		}
		int buydate = 0;
		if (StringUtils.isNotBlank(fromDate)) {
			buydate = Integer.valueOf(fromDate);
		}
		List<BuyTrace> list = buyTraceService.getListByCode("", buydate, 0, status, querypage);
		List<ViewVo> l = new LinkedList<ViewVo>();
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				BuyTrace bt = list.get(i);
				ViewVo vv = new ViewVo();
				BeanCopy.copy(bt, vv);
				vv.setIndex(i + 1);
				vv.setName(stockBasicService.getCodeName(vv.getCode()));
				if (vv.getSoldDate() <= 0) {
					SinaRealTime srt = SinaRealtimeUitl.get(vv.getCode());
					if (srt != null && srt.getBuy1() > 0.0) {
						vv.setSoldPrice(srt.getBuy1());
						vv.setProfit(CurrencyUitl.cutProfit(vv.getBuyPrice(), vv.getSoldPrice()));
					}
				}
				l.add(vv);
			}
		}
		return l;
	}

	public ReportVo report(String all, String fromDate) {
		ReportVo pv = new ReportVo();

		int status = 0;
		if (StringUtils.isNotBlank(all) && "1".equals(all)) {
			pv.setAll("全部");
		} else {
			status = TradeType.SOLD.getCode();
			pv.setAll("已成交");
		}
		int buydate = 0;
		if (StringUtils.isNotBlank(fromDate)) {
			buydate = Integer.valueOf(fromDate);
		}
		pv.setFromDate(fromDate);

		List<BuyTrace> list = buyTraceService.getListByCode("", buydate, 0, status, querypage);

		if (list != null) {
			int allc = 0;// 总数
			double allp = 0.0;// 总盈亏
			int ynowc = 0;// 盈利总数（未成交）
			int ynowp = 0;// 盈利的总盈亏（未成交）
			int sc = 0;// 已卖总数
			double sp = 0.0;// 已卖总盈亏
			int ysc = 0;// 已卖中盈利总数
			double ysp = 0.0;// 已卖中盈利总盈亏

			for (int i = 0; i < list.size(); i++) {
				BuyTrace bt = list.get(i);

				if (bt.getSoldDate() <= 0) {
					SinaRealTime srt = SinaRealtimeUitl.get(bt.getCode());
					if (srt != null && srt.getBuy1() > 0.0) {
						bt.setSoldPrice(srt.getBuy1());
						bt.setProfit(CurrencyUitl.cutProfit(bt.getBuyPrice(), bt.getSoldPrice()));
					}
				} else {
					sc++;
					sp += bt.getProfit();

					if (bt.getProfit() > 0.0) {
						ysc++;
						ysp += bt.getProfit();
					}
				}

				if (bt.getProfit() > 0.0) {
					ynowc++;
					ynowp += bt.getProfit();
				}
				allc++;
				allp += bt.getProfit();
			}

			pv.setAllCnt(allc);
			pv.setAllProfit(allp);
			pv.setSoldCnt(sc);
			pv.setSoldProfit(sp);
			pv.setYsoldCnt(ysc);
			pv.setYsoldProfit(ysp);
			pv.setSoldRate(CurrencyUitl.getRate(ysc, sc));

			pv.setYnowCnt(ynowc);
			pv.setYnowProfit(ynowp);
			pv.setNowRate(CurrencyUitl.getRate(ynowc, allc));
		}
		return pv;
	}
}
