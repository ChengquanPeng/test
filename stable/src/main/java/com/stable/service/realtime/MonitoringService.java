package com.stable.service.realtime;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.BuyModelType;
import com.stable.enums.TradeType;
import com.stable.service.CodePoolService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.trace.BuyTraceService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.bus.CodePool;
import com.stable.vo.bus.TickData;
import com.stable.vo.http.resp.ReportVo;
import com.stable.vo.http.resp.ViewVo;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MonitoringService {
	private static final String BR = "</br>";

	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private BuyTraceService buyTraceService;
	private Map<String, RealtimeDetailsAnalyzer> map = null;
	@Autowired
	private MonitoringSortV4Service monitoringSortV4Service;

	public synchronized void startObservable() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				monitoringSortV4Service.startObservable();
			}
		}).start();
//		if (System.currentTimeMillis() > 0) {
//			return;
//		}
		String date = DateUtil.getTodayYYYYMMDD();
		int idate = Integer.valueOf(date);
		if (!tradeCalService.isOpen(idate)) {
			// WxPushUtil.pushSystem1("非交易日结束监听");
			return;
		}
		long now = new Date().getTime();
		long isAlivingMillis = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 15:03:00").getTime();
		if (now > isAlivingMillis) {// 已经超时
			log.info("now > isAlivingMillis,已超时");
			return;
		}

		try {
			// 所有买卖Code List
			// 获取买入监听列表
			List<CodePool> allCode = codePoolService.getPoolListForMonitor();

			List<RealtimeDetailsAnalyzer> list = new LinkedList<RealtimeDetailsAnalyzer>();
			RealtimeDetailsResulter resulter = new RealtimeDetailsResulter();

			int failtt = 0;
			if (allCode.size() > 0) {
				// 启动监听线程
				map = new ConcurrentHashMap<String, RealtimeDetailsAnalyzer>();
				for (CodePool cp : allCode) {
					String code = cp.getCode();
					log.info(code);
					RealtimeDetailsAnalyzer task = new RealtimeDetailsAnalyzer();
					int r = task.init(code, cp, resulter, stockBasicService.getCodeName(code));
					if (r == 1) {
						new Thread(task).start();
						list.add(task);
						map.put(code, task);

					} else {
						if (r < 0) {
							failtt++;
						}
					}
				}
				// 启动结果线程
				if (list.size() > 0) {
					new Thread(resulter).start();
				}
			}

			WxPushUtil.pushSystem1(
					"交易日监听实时交易，监听总数:[" + allCode.size() + "],实际总数[" + list.size() + "],监听失败[" + failtt + "]");

			long from3 = new Date().getTime();
			int millis = (int) ((isAlivingMillis - from3));
			if (millis > 0) {
				Thread.sleep(millis);
			}
			// List<BuyTrace> buyedList = new LinkedList<BuyTrace>();
			// List<BuyTrace> selledList = new LinkedList<BuyTrace>();
			// 到点停止所有线程
			for (RealtimeDetailsAnalyzer t : list) {
				t.stop();
//				if (t.getBuyed() != null) {
//					buyedList.add(t.getBuyed());
//				}
//				if (t.getSelled() != null) {
//					selledList.add(t.getSelled());
//				}
			}
//			WxPushUtil.pushSystem2Html("交易日结束监听!");
			// sendEndMessaget(buyedList, selledList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			map = null;
		}
	}

	public void sendEndMessaget(List<BuyTrace> buyedList, List<BuyTrace> selledList) {
		StringBuffer sb = new StringBuffer("交易日结束监听,买入数量[" + buyedList.size() + "],卖出数量[" + selledList.size() + "]");
		sb.append(BR);
		int index = 1;
		if (buyedList.size() > 0) {
			sb.append("买入明细：==>").append(BR);
			for (BuyTrace x : buyedList) {
				sb.append("序号:").append(index).append(", ").append(x.getCode()).append(" ")
						.append(stockBasicService.getCodeName(x.getCode())).append(" 买入价格:").append(x.getBuyPrice())
						.append(" 买入时间:").append(x.getBuyDate()).append(BR);
				index++;
			}
		}
		if (selledList.size() > 0) {
			sb.append("卖出明细：==>").append(BR);
			for (BuyTrace x : selledList) {
				sb.append("序号:").append(index).append(", ").append(x.getCode()).append(" ")
						.append(stockBasicService.getCodeName(x.getCode())).append(" 买入时间:").append(x.getBuyDate())
						.append(" 买入价格:").append(x.getBuyPrice()).append(" 卖出价格:").append(x.getSoldPrice())
						.append(" 收益:").append(x.getProfit()).append("%").append(BR);
				index++;
			}
		}
		WxPushUtil.pushSystem2Html(sb.toString());
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
		return code + " " + stockBasicService.getCodeName(code) + "==> 当前信息(SINA):" + SinaRealtimeUitl.get(code);
	}

	public String sell(String code) {
		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt != null && srt.getSell1() > 0.0) {
			List<BuyTrace> list = buyTraceService.getListByCode(code, 0, TradeType.BOUGHT.getCode(),
					BuyModelType.B1.getCode(), 0, EsQueryPageUtil.queryPage9999);
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
			bt.setVer(0);
			bt.setSubVer(0);
			bt.setId();
			bt.setStatus(TradeType.BOUGHT.getCode());
			buyTraceService.addToTrace(bt);
			log.info("人工买已成交:{}" + bt);
			return bt;
		}
		return null;
	}

	public ReportVo getVeiw(String all, String type, String fromDate) {
		ReportVo pv = new ReportVo();

		int status = 0;
		if (StringUtils.isBlank(all) || "0".equals(all)) {
			pv.setAll("全部(未卖出)");
		} else if ("1".equals(all)) {
			status = TradeType.BOUGHT.getCode();
			pv.setAll("已买入");
		} else if ("2".equals(all)) {
			status = TradeType.SOLD.getCode();
			pv.setAll("已卖出");
		}
		int buydate = 0;
		if (StringUtils.isNotBlank(fromDate)) {
			buydate = Integer.valueOf(fromDate);
		}
		pv.setFromDate(fromDate);
		int buyModelType = 0;
		if (StringUtils.isNotBlank(type)) {
			buyModelType = Integer.valueOf(type);
			if (buyModelType == 1) {
				pv.setType("人工");
			} else if (buyModelType == 2) {
				pv.setType("机器");
			} else {
				pv.setType("未知" + buyModelType);
			}
		} else {
			pv.setType("全部");
		}

		List<BuyTrace> list = buyTraceService.getListByCode("", buydate, status, buyModelType, 0,
				EsQueryPageUtil.queryPage9999);
		List<ViewVo> l = new LinkedList<ViewVo>();
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

				// 明细
				ViewVo vv = new ViewVo();
				BeanCopy.copy(bt, vv);
				vv.setIndex(i + 1);
				vv.setName(stockBasicService.getCodeName(vv.getCode()));
				l.add(vv);
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
		pv.setList(l);
		return pv;
	}
}
