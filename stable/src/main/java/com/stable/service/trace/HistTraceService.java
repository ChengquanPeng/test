package com.stable.service.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.enums.StockAType;
import com.stable.es.dao.base.EsHistTraceDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LineVol;
import com.stable.service.model.data.StrongService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.LogFileUitl;
import com.stable.utils.TasksWorkerModel;
import com.stable.utils.TasksWorkerModelRunnable;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.HistTrace;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.retrace.TraceSortv2StatVo;
import com.stable.vo.retrace.TraceSortv2Vo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class HistTraceService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private EsHistTraceDao esHistTraceDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private StrongService strongService;

	private String FILE_FOLDER = "/my/free/pvhtml/";

	public static final Semaphore semp = new Semaphore(1);

	EsQueryPageReq queryPage250 = new EsQueryPageReq(250);
	EsQueryPageReq queryPage20 = new EsQueryPageReq(20);
	EsQueryPageReq queryPage5 = new EsQueryPageReq(5);

	public void sortv2(String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();

		if (StringUtils.isBlank(startDate)) {
			startDate = "20200101";
		}
		if (StringUtils.isBlank(endDate)) {
			endDate = DateUtil.getTodayYYYYMMDD();
		}
		log.info("startDate={},endDate={}", startDate, endDate);
		try {
			boolean getLock = semp.tryAcquire(1, TimeUnit.HOURS);
			if (!getLock) {
				log.warn("No Locked");
				return;
			}
			log.info("Get Locked");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		String sd = startDate;
		String ed = endDate;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String[] versions = { "v2", "v3" };
					int[] days = { 2, 3 };
					double[] volBases = { 0, 1.2, 1.3 };
//					int[] oneYearups = { 1 };// 1年未大涨的（TODO,第一波是否涨超30%？）
					for (String ver : versions) {
						for (double vb : volBases) {
							for (int d : days) {
								try {
									reallymodelForJob(ver, sd, ed, d, vb, sysstart, batch);
									ThreadsUtil.sleepRandomSecBetween15And30();
								} catch (Exception e) {
									e.printStackTrace();
									WxPushUtil.pushSystem1(
											versions + "样本区间:" + sd + " " + ed + "条件(" + (d) + "天期,交易量" + vb + ")样本出错");
								}
							} // for-days
						} // for-volbase
					} // versions
					WxPushUtil.pushSystem1("样本完成！" + sd + " " + ed);
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("样本出错！" + sd + " " + ed);
				} finally {
					semp.release();
				}
			}
		}).start();

	}

	private TraceSortv2Vo runinner(String code, int date, double min, double max, int day, double vb, boolean isV2,
			double todayChangeRate, double yesterdayPrice, double closedPrice, double highPrice) {
		try {
			log.info("code={},date={}", code, date);
			// 1.上涨且未超过8%
			if (todayChangeRate >= min && todayChangeRate <= max) {
				List<DaliyBasicInfo> dailyList = daliyBasicHistroyService
						.queryListByCodeForModel(code, date, queryPage250).getContent();
				LineVol lineVol = new LineVol(dailyList);
				// 缩量
				if (vb == 0 || (lineVol.isShortVolThanYerteryDay() && lineVol.isShortVolV2(vb))) {// 2.没有超过5天均量1.3倍,今天的量比昨天高
					LinePrice linePrice = new LinePrice(code, date, daliyTradeHistroyService);

					boolean b6 = linePrice.checkPriceBack6dayWhitTodayV2();// 5.回调过超10%
//					boolean b5 = linePrice.check3dayPriceV2();// 6.对比3天-价
					boolean b4 = linePrice.isLowClosePriceToday(todayChangeRate, yesterdayPrice, closedPrice,
							highPrice);// 上影线
					if (b6 && !b4 && (isV2 ? linePrice.check3dayPriceV2() : true)) {
						LineAvgPrice lineAvg = new LineAvgPrice(code, date, avgService, dailyList);
						if (lineAvg.feedData()) {
							if (lineAvg.isWhiteHorseV2()) {
								if (isUpNline(isV2, lineAvg, yesterdayPrice, closedPrice)) {
									if (linePrice.oneYearCheck(code, date)) {
										return saveOkRec(code, date, day);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, code, code, date + "");
		}
		return null;
	}

	public synchronized void reallymodelForJob(String ver, String startDate, String endDate, int d, double vb,
			String sysstart, int batch) {
		reallymodelForRealTime(ver, startDate, endDate, d, vb, sysstart, batch);
	}

	public void reallymodelForRealTime(String ver, String startDate, String endDate, int d, double vb, String sysstart,
			int batch) {
		boolean isV2 = "v2".equals(ver);
		String version = (isV2 ? "v2" : "v3");
		int day = d + 1;
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		List<TraceSortv2Vo> samples = Collections.synchronizedList(new LinkedList<TraceSortv2Vo>());
		double min = 4.5;
		double max = 9.0;
		if (isV2) {
			min = 0.0;
			max = 8.0;
		}
		double fmin = min;
		double fmax = max;
		int sd = Integer.valueOf(startDate);
		// start..
		log.info("codelist:" + codelist.size());
		CountDownLatch cnt = new CountDownLatch(codelist.size());
		for (StockBaseInfo s : codelist) {
			StockAType sa = StockAType.formatCode(s.getCode());
			if (StockAType.KCB == sa) {
				cnt.countDown();
				continue;
			}
			String code = s.getCode();
			boolean onlineYear = stockBasicService.online1Year(code, sd);
			if (!onlineYear) {
				cnt.countDown();
				continue;
			}
			try {
				TasksWorkerModel.add(code, new TasksWorkerModelRunnable() {
					public void running() {
						try {
							JSONArray array2 = tushareSpider.getStockDaliyTrade(s.getTs_code(), null, startDate,
									endDate);
							if (array2 != null && array2.size() > 0) {
								// codesamples:整个代码区间的（用于去重）
								// samples:所有代码用于统计
								List<TraceSortv2Vo> codesamples = Collections
										.synchronizedList(new LinkedList<TraceSortv2Vo>());
								for (int ij = (array2.size() - 1); ij >= 0; ij--) {
									DaliyBasicInfo d2 = new DaliyBasicInfo();
									d2.setCode(code);
									d2.daily(array2.getJSONArray(ij));
									if (checkExist(codesamples, d2.getTrade_date(), day)) {
										continue;
									}
									TraceSortv2Vo t1 = runinner(d2.getCode(), d2.getTrade_date(), fmin, fmax, day, vb,
											isV2, d2.getTodayChangeRate(), d2.getYesterdayPrice(), d2.getClose(),
											d2.getHigh());
									if (t1 != null) {
										codesamples.add(t1);
									}
								}
								if (codesamples.size() > 0) {
									samples.addAll(codesamples);
								}
							}
						} finally {
							cnt.countDown();
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				ErrorLogFileUitl.writeError(e, code, "", "");
			}
		}
		try {
			cnt.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		log.info(version + "获取样本数:" + samples.size());

		int total_all = samples.size();
		if (total_all > 0) {
			TraceSortv2StatVo stat = new TraceSortv2StatVo();
			String filepath = FILE_FOLDER + version + "_" + startDate + "_" + endDate + "_" + d + "_" + +vb + "_"
					+ batch;
			stat(filepath, stat, samples);
			sendMessge(version, batch, startDate, endDate, d, vb, stat, total_all, sysstart);
		} else {
			WxPushUtil.pushSystem1(
					version + "样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb + ")样本数量:" + total_all);
		}

	}

	private boolean isUpNline(boolean isV2, LineAvgPrice lineAvg, double yesterdayPrice, double closedPrice) {
		if (isV2) {
			StockAvg av = lineAvg.todayAv;
			return (av.getAvgPriceIndex5() > yesterdayPrice || av.getAvgPriceIndex10() > yesterdayPrice
					|| av.getAvgPriceIndex20() > yesterdayPrice || av.getAvgPriceIndex30() > yesterdayPrice//
			)// 4.昨日收盘价在任意均线之下
					&& (closedPrice > av.getAvgPriceIndex5() && closedPrice > av.getAvgPriceIndex10()
							&& closedPrice > av.getAvgPriceIndex20() && closedPrice > av.getAvgPriceIndex30()//
					);//
		}
		return true;
	}

	private boolean checkExist(List<TraceSortv2Vo> codesamples, int date, int day) {
		if (codesamples != null && codesamples.size() > 0) {
			TraceSortv2Vo tsv = codesamples.get(codesamples.size() - 1);
			// tsv.getDate()：目前最后的日期
			// tsv.getDate()+day：相加后的日期
			// 相加后的日期：在这日期之前不允许存在
			int day30 = Integer.valueOf(DateUtil.formatYYYYMMDD(DateUtil.addDate(tsv.getDate() + "", day)));
			if (day30 > date) {// 30天之内存在则不能在作为样本
				return true;
			}
		}
		return false;
	}

	private TraceSortv2Vo saveOkRec(String code, int date, int day) {
		try {
			EsQueryPageReq queryPage6 = new EsQueryPageReq(day);
			TraceSortv2Vo t1 = new TraceSortv2Vo();
			t1.setCode(code);
			t1.setDate(date);
			t1.setMarketIndex(strongService.getIndexPrice(code, date));
			//
			List<TradeHistInfoDaliy> dailyList2 = new ArrayList<TradeHistInfoDaliy>();
			// date=已收盘的日期
			List<TradeHistInfoDaliy> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, date, 0,
					queryPage6, SortOrder.ASC);// 返回的list是不可修改对象
			for (int i = 1; i < dailyList0.size(); i++) {
				dailyList2.add(dailyList0.get(i));
			}
			// i=0;是当天
			TradeHistInfoDaliy d0 = dailyList0.get(0);
			// 最高盈利
			double maxPrice = dailyList2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed)).get()
					.getClosed();
			// 最高亏损
			double minPrice = dailyList2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed)).get()
					.getClosed();
			// 最低价格
			double minLowPrice = dailyList2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get()
					.getLow();
			t1.setMinLowPrice(minLowPrice);
			t1.setBuyPrice(d0.getClosed());
			t1.setMaxPrice(maxPrice);
			t1.setMinPrice(minPrice);
			t1.setSellPrice(dailyList0.get(dailyList0.size() - 1).getClosed());
			t1.setBuyDayRate(d0.getTodayChangeRate());
			DaliyBasicInfo basic = daliyBasicHistroyService.spiderStockDaliyBasicForOne(code, date + "", date + "");
			t1.setDb(basic);

			// 最新财务
			FinanceBaseInfo fin = financeService.getLastFinaceReport(code, date);
			t1.setFin(fin);
			// 最新快报&预告
			int currYear = DateUtil.getYear(date);
			int currJidu = DateUtil.getJidu(date);
			// 当前季度的快预报（当前季度中后期）
			if (!getLastKygb(t1, code, date, currYear, currJidu)) {
				int preYear = currYear;
				int preJidu = currJidu - 1;
				if (preJidu == 0) {
					preYear = preYear - 1;
					preJidu = 4;
				}
				// 上季度的快预报（当前季度初期）
				if (!getLastKygb(t1, code, date, preYear, preJidu)) {

					// 财务季度的快预报
					if (fin != null) {
						int finYear = DateUtil.getYear(fin.getDate());
						int finJidu = DateUtil.getJidu(fin.getDate());
						getLastKygb(t1, code, date, finYear, finJidu);
					}
				}
			}
			return t1;
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, code, date + "", "");
			e.printStackTrace();
		}
		return null;
	}

	private boolean getLastKygb(TraceSortv2Vo t1, String code, int date, int year, int jidu) {
		FinYjkb ckb = financeService.getLastFinYjkbReport(code, date, year, jidu);
		if (ckb == null) {
			FinYjyg cyg = financeService.getLastFinYjygReport(code, date, year, jidu);
			if (cyg != null) {
				t1.setYg(cyg);
				return true;
			}
		} else {
			t1.setKb(ckb);
			return true;
		}
		return false;
	}

	private void stat(String filepath, TraceSortv2StatVo stat, List<TraceSortv2Vo> samples) {
		StringBuffer sb = new StringBuffer();
		for (TraceSortv2Vo t1 : samples) {
			sb.append(t1.toExcel()).append(FileWriteUitl.LINE_FILE);

			// 理论最高盈利
			double profit = CurrencyUitl.cutProfit(t1.getBuyPrice(), t1.getMaxPrice());
			stat.setTotalProfit(stat.getTotalProfit() + profit);
			if (profit > 0) {
				stat.setCnt_up(stat.getCnt_up() + 1);
			}
			// 理论最高亏损
			double loss = CurrencyUitl.cutProfit(t1.getBuyPrice(), t1.getMinPrice());
			stat.setTotalLoss(stat.getTotalLoss() + loss);
			if (loss < 0) {
				stat.setCnt_down(stat.getCnt_down() + 1);
			} else {
				// 理论最高亏损分布
				stat.statLossLowClosedPrice(loss);
			}
			// 实际盈亏
			double act_profit = t1.getActProfit();
			stat.setAct_totalProfit(stat.getAct_totalProfit() + act_profit);
			if (act_profit > 0) {
				stat.setAct_cnt_up(stat.getAct_cnt_up() + 1);
			} else {
				// 实际亏损分布
				stat.statLossAct(act_profit);
			}
			// 理论最低价分布区间
			double low_loss = CurrencyUitl.cutProfit(t1.getBuyPrice(), t1.getMinLowPrice());
			if (low_loss < 0) {
				stat.statLossLowPrice(low_loss);
			}
		}
		if (sb.length() > 0) {
			LogFileUitl.writeLog(filepath + ".csv", sb.toString());
		}
	}

	private void sendMessge(String vers, int batch, String startDate, String endDate, int d, double vb,
			TraceSortv2StatVo stat, int total_all, String sysstart) {

		HistTrace ht = new HistTrace();
		ht.setBatch(batch);
		ht.setStartDate(Integer.valueOf(startDate));
		ht.setEndDate(Integer.valueOf(endDate));
		ht.setVersion(vers);
		ht.setDays(d);
		ht.setVol(vb);

		ht.setTotalAll(total_all);
		ht.setTotalLoss(stat.getTotalLoss());
		ht.setTotalProfit(stat.getTotalProfit());
		ht.setAct_cnt_up(stat.getAct_cnt_up());
		ht.setAct_totalProfit(stat.getAct_totalProfit());
		ht.setLossSettAct(stat.getStatLossAct());
		ht.setLossSettClosedPrice(stat.getStatLossLowClosedPrice());
		ht.setLossSettLowPrice(stat.getStatLossLowPrice());
		ht.setId(UUID.randomUUID().toString());
		esHistTraceDao.save(ht);

		WxPushUtil.pushSystem1(
				vers + " 样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb + ")样本数量:" + total_all//
						+ ",[理论最高盈利]次数:" + stat.getCnt_up() + ",盈利概率:"
						+ CurrencyUitl.roundHalfUpWhithPercent(stat.getCnt_up() / Double.valueOf(total_all)) + "%"
						+ ",总盈利百分比:" + CurrencyUitl.roundHalfUp(stat.getTotalProfit()) //
						+ "%,[理论最高亏损]次数:" + stat.getCnt_down() + ",亏损概率:"
						+ CurrencyUitl.roundHalfUpWhithPercent(stat.getCnt_down() / Double.valueOf(total_all)) + "%"
						+ ",总亏损百分比:" + CurrencyUitl.roundHalfUp(stat.getTotalLoss()) //
						+ "%,[实际盈利]次数:" + stat.getAct_cnt_up() + ",盈利概率:"
						+ CurrencyUitl.roundHalfUpWhithPercent(stat.getAct_cnt_up() / Double.valueOf(total_all)) + "%"
						+ ",实际盈亏百分比:" + CurrencyUitl.roundHalfUp(stat.getAct_totalProfit()) + "%" //
						// 盈利
						+ "[实际亏损分布]" + ht.getLossSettAct() //
						+ "[收盘价亏损分布]" + ht.getLossSettClosedPrice() //
						+ "[最低价亏损分布]" + ht.getLossSettLowPrice() //
						// 亏损
						+ ",开始时间:" + sysstart//
		);
	}
}
