package com.stable.service.realtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.config.SpringConfig;
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.BuyModelType;
import com.stable.enums.ModelType;
import com.stable.enums.TradeType;
import com.stable.service.ConceptService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.UpModelLineService;
import com.stable.service.trace.BuyTraceService;
import com.stable.service.trace.SortV4Service;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.ScheduledWorker;
import com.stable.utils.SpringUtil;
import com.stable.utils.TasksWorkerModel;
import com.stable.utils.TasksWorkerModelRunnable;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.SortV4Reslt;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MonitoringSortV4Service {
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private UpModelLineService upModelLineService;
	@Autowired
	private SortV4Service sortV4Service;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private BuyTraceService buyTraceService;

	private Map<String, Integer> sign = new ConcurrentHashMap<String, Integer>();

	public synchronized void startObservable() {
		sign.clear();
		String date = DateUtil.getTodayYYYYMMDD();
		int today = Integer.valueOf(date);
		if (!tradeCalService.isOpen(today)) {
			// WxPushUtil.pushSystem1("非交易日结束监听");
			return;
		}
		long now = new Date().getTime();
		long isAlivingMillis = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 15:03:00").getTime();
		if (now > isAlivingMillis) {// 已经超时
			log.info("now > isAlivingMillis,已超时");
			return;
		}
		int cnt = 0;
		String observableDate = tradeCalService.getPretradeDate(date);
		try {
			log.info("observableDate sortV4:" + observableDate);
			// 获取买入监听列表
			List<ModelV1> bList = upModelLineService.getListByCode(null, observableDate, null, null, null,
					EsQueryPageUtil.queryPage9999, ModelType.V4.getCode());

			if (bList == null || bList.size() <= 0) {
				log.info("获取买入监听列表空observableDate={}", observableDate);
				return;
			}
			Map<String, Integer> yesterdayCondi = new ConcurrentHashMap<String, Integer>();
			Map<String, Integer> stopset = new ConcurrentHashMap<String, Integer>();

			// 定点刷新及通知
			Date msgtime = DateUtil.parseDate(date + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
			long d145000 = msgtime.getTime();
			if (now <= d145000) {
				ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
					@Override
					public void run() {
						int sz = start(bList, yesterdayCondi, stopset).size();
						WxPushUtil.pushSystem1("sortV4 实时分析已经生成总数:" + sz);
					}
				}, msgtime);
				log.info("scheduled Task with Time:{}", msgtime);
			}
			// 盘中5分钟刷新
			long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
					DateUtil.parseDate(date + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
			Date dt13 = DateUtil.parseDate(date + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
			long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(dt13);

			long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(msgtime);
			long d145800 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
					DateUtil.parseDate(date + "145800", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));

			SortV4Reslt v4rest = null;

			while (true) {
				now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
				if (now >= d145800) {
					break;
				}
				v4rest = start(bList, yesterdayCondi, stopset);
				if (d1130 <= now && now <= d1300) {
					long from3 = new Date().getTime();
					long millis = (dt13.getTime() - from3);
					if (millis > 0) {
						log.info("sortv4 中场休息。");
						Thread.sleep(millis);
					}
				} else {
					if (d1450 <= now && now <= d145800) {
						ThreadsUtil.sleep(1, TimeUnit.MINUTES);
					} else {
						ThreadsUtil.sleep(5, TimeUnit.MINUTES);
					}
				}
			}
			// ScheduledWorker.addScheduled(command, 5, TimeUnit.MINUTES);

//			long d1140 = d2.getTime();
//			if (now <= d1140) {
//				ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
//					@Override
//					public void run() {
//						start(bList);
//					}
//				}, d2);
//				log.info("scheduled Task with Time:{}", d2);
//			} else {
//				start(bList);
//			}
			List<ModelSortV4> oklist = v4rest.getLn().get(1);
			if (oklist != null) {
				List<BuyTrace> bts = new ArrayList<BuyTrace>();
				for (ModelSortV4 v4 : oklist) {
					if (v4.getBuyPirce() > 0) {
						BuyTrace bt = new BuyTrace();
						bt.setBuyDate(today);
						bt.setBuyModelType(BuyModelType.B2.getCode());
						bt.setBuyPrice(v4.getBuyPirce());
						bt.setCode(v4.getCode());
						bt.setStatus(TradeType.BOUGHT.getCode());
						bt.setVer(ModelType.V4.getCode());
						bt.setSubVer(0);
						bt.setId();
						bts.add(bt);
					}
				}
				if (bts.size() > 0) {
					buyTraceService.addToTrace(bts);
					cnt = bts.size();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		WxPushUtil.pushSystem1("sortV4 模型监听结束,买入笔数:" + cnt);
	}

	private double minRate = 3.2;
	private double maxRate = 6.5;

	private SortV4Reslt start(List<ModelV1> bList, Map<String, Integer> yesterdayCondi, Map<String, Integer> stopset) {
		SortV4Reslt oklist = new SortV4Reslt();
		CountDownLatch cnt = new CountDownLatch(bList.size());
		for (ModelV1 ss : bList) {
			String code = ss.getCode();
			int date = ss.getDate();
			if (!stopset.containsKey(code)) {
				TasksWorkerModel.add(code, new TasksWorkerModelRunnable() {
					@Override
					public void running() {
						try {
							SinaRealTime srt = SinaRealtimeUitl.get(code);
							if (srt.getOpen() == 0.0) {
								log.info("{} SINA 今日停牌", code);
								stopset.put(code, 1);
								return;
							}
							double yesterdayPrice = srt.getYesterday();
							double closedPrice = srt.getNow();
							if (closedPrice < yesterdayPrice) {
								log.info("{} closedPrice={},yesterdayPrice={},下跌", code, closedPrice, yesterdayPrice);
								return;
							}
							double todayChangeRate = CurrencyUitl.cutProfit(yesterdayPrice, closedPrice);
							if (todayChangeRate >= minRate) {
								Integer checked = yesterdayCondi.get(code);
								if (checked == null) {
									checked = 0;
									if (sortV4Service.isTradeOkBefor5ForPrice(code, date, false)) {
										if (sortV4Service.isWhiteHorseForSortV4(code, date, false)) {
											checked = 1;
										} else {
											log.info("{} 均线支持不合适", code, todayChangeRate);
										}
									} else {
										log.info("{} 前5个交易日不合适", code, todayChangeRate);
									}
									yesterdayCondi.put(code, checked);
								}
								if (checked == 1) {
									if (sortV4Service.isTodayPriceOk(minRate, maxRate, todayChangeRate, yesterdayPrice,
											closedPrice, srt.getHigh())) {

										ModelSortV4 v4 = new ModelSortV4();
										BeanCopy.copy(ss, v4);
										v4.setBuyPirce(srt.getSell1());
										v4.setTodayChange(todayChangeRate);
										v4.setGn(conceptService.getCodeConceptForCode(code));

										// 是否放量

										if (sortV4Service.isTradeOkBefor5ForVol(code, date, false, yesterdayPrice,
												srt.getHigh(), srt.getDealNums())) {
//											if(sortV4Service.priceCheckForSortV4(code, date)) {
//												
//											}
											v4.setLevel(1);
										} else {
											v4.setLevel(4);
										}
										oklist.add(v4);

									} else {
										log.info("{} todayChangeRate={},价格涨幅过大或上影线", code, todayChangeRate);
										ModelSortV4 v4 = new ModelSortV4();
										BeanCopy.copy(ss, v4);
										v4.setBuyPirce(srt.getSell1());
										v4.setTodayChange(todayChangeRate);
										v4.setGn(conceptService.getCodeConceptForCode(code));
										v4.setLevel(todayChangeRate > maxRate ? 2 : 3);
										oklist.add(v4);
									}
								}

							} else {
								log.info("{} todayChangeRate={},价格不满足最低", code, todayChangeRate);
							}
						} finally {
							cnt.countDown();
						}
					}
				});
			} else {
				cnt.countDown();
			}
		}
		try {
			if (!cnt.await(6, TimeUnit.MINUTES)) {// 等待执行完成
				log.info("SortV4 Reattime执行完成超时异常==>");
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		fulshToFile(oklist);
		return oklist;
	}

	private String header = "<table border='1' cellspacing='0' cellpadding='0'><tr>";
	private String endder = "</table><script type='text/javascript' src='/html/static/addrlurl.js'></script>";
//	private String endder = "</table>";

	public MonitoringSortV4Service() {
		String[] s = { "序号", "代码", "简称", "日期", "综合评分", "基本面评分", "短期强势", "主力行为", "流通市值", "市盈率(静/ttm)", "今日涨幅",
				"分级(1,2-涨,3,4-量)", "标记", "概念" };
		for (int i = 0; i < s.length; i++) {
			header += this.getHTMLTH(s[i]);
		}
		header += "</tr>" + FileWriteUitl.LINE_FILE;
	}

	public void fulshToFile(SortV4Reslt saveList) {
		log.info("saveList size:{}", saveList.size());
		saveList.sort();
		StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);

		StringBuffer sb = new StringBuffer();
		sb.append("<div>更新时间:" + DateUtil.getTodayYYYYMMDDHHMMSS() + " 其他条件:高位、热门股等等</div><br/>");
		sb.append(header);
		// StringBuffer sb2 = new StringBuffer(header);
		int index = 1;
		if (saveList.size() > 0) {
			for (List<ModelSortV4> ln : saveList.getLn().values()) {
				for (ModelSortV4 mv : ln) {
					String code = mv.getCode();
					DaliyBasicInfo basic = daliyBasicHistroyService.getDaliyBasicInfoByDate(code, mv.getDate());
					sb.append("<tr>").append(getHTML(index)).append(getHTML_SN(code))
							.append(getHTML(sbs.getCodeName(code))).append(getHTML(mv.getDate()))
							.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
							.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
							.append(getHTML(CurrencyUitl.covertToString(basic.getCirc_mv() * 10000)))
							.append(getHTML(basic.getPe() + "/" + basic.getPe_ttm()))
							.append(getHTML(mv.getTodayChange())).append(getHTML(mv.getLevel()))
							.append(getSignUrl(code)).append(getHTML(mv.getGn())).append("</tr>")
							.append(FileWriteUitl.LINE_FILE);

//				sb2.append("<tr>").append(getHTML(index)).append(getHTML_SN(code))
//						.append(getHTML(sbs.getCodeName(code))).append(getHTML(mv.getDate()))
//						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
//						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
//						.append(getHTML(mv.getSortWay())).append(getHTML(mv.getPriceIndex()))
//						.append(getHTML(result.get(code))).append("</tr>").append(FileWriteUitl.LINE_FILE);
					index++;
				}
			}
		}
		sb.append(endder);
		// sb2.append(endder);

//			String filepath = efc.getModelV1SortFloder() + "sortv4today.html";
		String filepath = efc.getPubFloder() + "sortv4today.html";
		FileWriteUitl fw = new FileWriteUitl(filepath, true);
		fw.writeLine(sb.toString());
		fw.close();

//			String filepath2 = efc.getModelV1SortFloderDesc() + "sortv1.html";
//			FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
//			fw2.writeLine(sb2.toString());
//			fw2.close();

		// fulshToFile2();
	}

	private String getSignUrl(String code) {
		Integer s = sign.get(code);
		if (s == null) {
			return getHTML_myrt(code);
		} else {
			return getHTML(s);
		}
	}

	private String getHTML(Object text) {
		return "<td>" + text + "</td>";
	}

	private String getHTML_SN(Object text) {
		return "<td class='sn'>" + text + "</td>";
	}

	private String getHTML_myrt(Object text) {
		return "<td class='myrt'>" + text + "</td>";
	}

	private String getHTMLTH(Object text) {
		return "<th>" + text + "</th>";
	}

	public void autoSell(int today) {
		int day = 3;
		int ds = day + 1;
		String p1 = tradeCalService.getPretradeDate(today + "");
		String p2 = tradeCalService.getPretradeDate(p1);
		String p3 = tradeCalService.getPretradeDate(p2);
		int chkDate = Integer.valueOf(p3);
		List<BuyTrace> bts = buyTraceService.getListByCode(null, 0, TradeType.BOUGHT.getCode(),
				BuyModelType.B2.getCode(), ModelType.V4.getCode(), EsQueryPageUtil.queryPage9999);
		int cnt = 0;
		if (bts != null && bts.size() > 0) {
			List<BuyTrace> upb = new ArrayList<BuyTrace>();
			for (BuyTrace bt : bts) {
				if (bt.getBuyDate() <= chkDate) {
					// date=已收盘的日期
					List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(
							bt.getCode(), bt.getBuyDate(), 0, EsQueryPageUtil.queryPage9999, SortOrder.ASC);// 返回的list是不可修改对象
					if (dailyList0.size() >= ds) {
						// 除去当天的交易日来算
						List<TradeHistInfoDaliyNofq> dailyList2 = new ArrayList<TradeHistInfoDaliyNofq>();
						for (int i = 1; i < dailyList0.size(); i++) {
							dailyList2.add(dailyList0.get(i));
						}
						// i=0;是当天
						TradeHistInfoDaliyNofq d0 = dailyList0.get(day);
						bt.setSoldDate(today);
						bt.setSoldPrice(d0.getClosed());
						bt.setProfit(CurrencyUitl.cutProfit(bt.getBuyPrice(), bt.getSoldPrice()));
						bt.setStatus(TradeType.SOLD.getCode());
						upb.add(bt);
					}
				}
			}
			if (upb.size() > 0) {
				buyTraceService.addToTrace(upb);
				cnt = upb.size();
			}
		}
		WxPushUtil.pushSystem1("sortV4 模型自动卖出笔数:" + cnt);
	}

	public void signCodeOk(String code, Integer s) {
		sign.put(code, s);
	}
}
