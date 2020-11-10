package com.stable.service.realtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.config.SpringConfig;
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.ModelType;
import com.stable.service.ConceptService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.UpModelLineService;
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
			log.info("observableDate sortV4:" + observableDate);
			// 获取买入监听列表
			List<ModelV1> bList = upModelLineService.getListByCode(null, observableDate, null, null, null,
					EsQueryPageUtil.queryPage9999, ModelType.V4.getCode());

			if (bList == null || bList.size() <= 0) {
				log.info("获取买入监听列表空observableDate={}", observableDate);
				return;
			}

			// 定点刷新及通知
			Date msgtime = DateUtil.parseDate(date + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
			long d145000 = msgtime.getTime();
			if (now <= d145000) {
				ScheduledWorker.scheduledTimeAndTask(new TimerTask() {
					@Override
					public void run() {
						start(bList);
						int sz = start(bList).size();
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
			long end = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
					DateUtil.parseDate(date + "150000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));

			long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(msgtime);
			long d1457 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
					DateUtil.parseDate(date + "145700", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
			while (true) {
				now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
				if (now >= end) {
					break;
				}
				start(bList);
				if (d1130 <= now && now <= d1300) {
					long from3 = new Date().getTime();
					long millis = (dt13.getTime() - from3);
					if (millis > 0) {
						log.info("sortv4 中场休息。");
						Thread.sleep(millis);
					}
				} else {
					if (d1450 <= now && now <= d1457) {
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

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		log.info("sort v4 结束");
	}

	private double minRate = 3.5;
	private double maxRate = 6.5;

	private List<ModelSortV4> start(List<ModelV1> bList) {
		List<ModelSortV4> oklist = Collections.synchronizedList(new LinkedList<ModelSortV4>());
		CountDownLatch cnt = new CountDownLatch(bList.size());
		for (ModelV1 ss : bList) {
			String code = ss.getCode();
			TasksWorkerModel.add(code, new TasksWorkerModelRunnable() {
				@Override
				public void running() {
					try {
						SinaRealTime srt = SinaRealtimeUitl.get(code);
						if (srt.getOpen() == 0.0) {
							log.info("{} SINA 今日停牌", code);
							return;
						}
						double yesterdayPrice = srt.getYesterday();
						double closedPrice = srt.getNow();
						if (closedPrice < yesterdayPrice) {
							log.info("{} closedPrice={},yesterdayPrice={},下跌", code, closedPrice, yesterdayPrice);
						}
						double todayChangeRate = CurrencyUitl.cutProfit(yesterdayPrice, closedPrice);
						if (sortV4Service.isTodayPriceOk(minRate, maxRate, todayChangeRate, yesterdayPrice, closedPrice,
								srt.getHigh())) {
							List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService
									.queryListByCodeWithLastNofq(code, 0, ss.getDate(), EsQueryPageUtil.queryPage5,
											SortOrder.DESC);// 返回的list是不可修改对象
							long deal = srt.getDealNums();
							double total = 0.0;
							for (TradeHistInfoDaliyNofq d : dailyList0) {
								total += d.getVolume();
							}
							// 均值*基数
							double chkVol = Double.valueOf(total / dailyList0.size() * 1.75);
							if (deal > chkVol) {
								log.info("今日明显放量近1倍 {},", code);
								// 明显放量的不能买
								return;
							}
							if (sortV4Service.isTradeOkBefor5(code, ss.getDate(), false)) {
								if (sortV4Service.isWhiteHorseForSortV4(code, ss.getDate(), false)) {
									ModelSortV4 v4 = new ModelSortV4();
									BeanCopy.copy(ss, v4);
									v4.setTodayChange(todayChangeRate);
									v4.setGn(conceptService.getCodeConceptForCode(code));
									oklist.add(v4);
								} else {
									log.info("{} 均线支持不合适", code, todayChangeRate);
								}
							} else {
								log.info("{} 前5个交易日不合适", code, todayChangeRate);
							}
						} else {
							log.info("{} todayChangeRate={},价格不合适", code, todayChangeRate);
						}
					} finally {
						cnt.countDown();
					}
				}
			});
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
	// private String endder = "</table><script type='text/javascript'
	// src='/html/static/addsinaurl.js'></script>";
	private String endder = "</table>";

	public MonitoringSortV4Service() {
		String[] s = { "序号", "代码", "简称", "日期", "综合评分", "基本面评分", "短期强势", "主力行为", "流通市值", "今日涨幅", "概念" };
		for (int i = 0; i < s.length; i++) {
			header += this.getHTMLTH(s[i]);
		}
		header += "</tr>" + FileWriteUitl.LINE_FILE;
	}

	public void fulshToFile(List<ModelSortV4> saveList) {

		log.info("saveList size:{}", saveList.size());

		StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
		sort(saveList);
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);

		StringBuffer sb = new StringBuffer();
		sb.append("<div>更新时间:" + DateUtil.getTodayYYYYMMDDHHMMSS() + "</div>");
		sb.append(header);
		// StringBuffer sb2 = new StringBuffer(header);
		int index = 1;
		if (saveList.size() > 0) {
			for (ModelSortV4 mv : saveList) {
				String code = mv.getCode();
				sb.append("<tr>").append(getHTML(index)).append(getHTML_SN(code)).append(getHTML(sbs.getCodeName(code)))
						.append(getHTML(mv.getDate())).append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
						.append(getHTML(CurrencyUitl.covertToString(
								daliyBasicHistroyService.getDaliyBasicInfoByDate(code, mv.getDate()).getCirc_mv()
										* 10000)))
						.append(getHTML(mv.getTodayChange())).append(getHTML(mv.getGn())).append("</tr>")
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

	private void sort(List<ModelSortV4> set) {
		Collections.sort(set, new Comparator<ModelSortV4>() {
			@Override
			public int compare(ModelSortV4 o1, ModelSortV4 o2) {
				return o2.getScore() - o1.getScore();
			}
		});
	}

	private String getHTML(Object text) {
		return "<td>" + text + "</td>";
	}

	private String getHTML_SN(Object text) {
		return "<td class='sn'>" + text + "</td>";
	}

	private String getHTMLTH(Object text) {
		return "<th>" + text + "</th>";
	}
}
