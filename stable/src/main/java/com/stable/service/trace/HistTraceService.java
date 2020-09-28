package com.stable.service.trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
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
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.LogFileUitl;
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

	private String FILE_FOLDER = "/my/free/pvhtml/";

	public static final Semaphore sempv3 = new Semaphore(1);
	public static final Semaphore sempv2 = new Semaphore(1);
	public static final Semaphore sempv1 = new Semaphore(1);

	EsQueryPageReq queryPage250 = new EsQueryPageReq(250);
	EsQueryPageReq queryPage20 = new EsQueryPageReq(20);
	EsQueryPageReq queryPage5 = new EsQueryPageReq(5);

	/**
	 * 右侧交易：涨幅超4.5%以上
	 */
	public void sortv3(String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
		try {
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
			if (StringUtils.isBlank(startDate)) {
				startDate = "20200101";
			}
			if (StringUtils.isBlank(endDate)) {
				endDate = DateUtil.getTodayYYYYMMDD();
			}
			log.info("startDate={},endDate={}", startDate, endDate);
			try {
				boolean getLock = sempv3.tryAcquire(1, TimeUnit.HOURS);
				if (!getLock) {
					log.warn("No Locked");
					return;
				}
				log.info("Get Locked");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			int[] days = { 2, 3 };
			double[] volBases = { 0, 1.2, 1.3 };
			int[] oneYearups = { 1 };// 1年未大涨的（TODO,第一波是否涨超30%？）
			for (int oneYear : oneYearups) {
				for (double vb : volBases) {
					for (int d : days) {
						try {
							v3Really(startDate, endDate, codelist, d, oneYear, vb, sysstart, batch);
						} catch (Exception e) {
							e.printStackTrace();
							WxPushUtil.pushSystem1("v3样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb
									+ ",一年涨幅限制" + oneYear + ")样本出错");
						}
					} // for-days
				} // for-volbase
			} // for-oneYears
			WxPushUtil.pushSystem1("v3样本完成！" + startDate + " " + endDate);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("v3样本出错！" + startDate + " " + endDate);
		} finally {
			sempv3.release();
		}
	}

	public void v3Really(String startDate, String endDate, List<StockBaseInfo> codelist, int d, int oneYear, double vb,
			String sysstart, int batch) {

		int day = d + 1;

		List<TraceSortv2Vo> samples = new LinkedList<TraceSortv2Vo>();
		// start..
		for (StockBaseInfo s : codelist) {
			StockAType sa = StockAType.formatCode(s.getCode());
			if (StockAType.KCB == sa) {
				continue;
			}
			String code = s.getCode();
			boolean onlineYear = stockBasicService.online1Year(code);
			if (!onlineYear) {
				// log.info("{},Online 上市不足1年", code);
				continue;
			}
			List<TraceSortv2Vo> codesamples = new LinkedList<TraceSortv2Vo>();

			JSONArray array2 = tushareSpider.getStockDaliyTrade(s.getTs_code(), null, startDate, endDate);
			if (array2 != null && array2.size() > 0) {
				for (int ij = (array2.size() - 1); ij >= 0; ij--) {
					DaliyBasicInfo d2 = new DaliyBasicInfo();
					d2.setCode(code);
					d2.daily(array2.getJSONArray(ij));
					int date = d2.getTrade_date();
					log.info("code={},date={}", code, date);
					try {
						// 1.上涨且未超过8%
						if (d2.getTodayChangeRate() > 4.5 && d2.getTodayChangeRate() <= 9.0) {
							List<DaliyBasicInfo> dailyList = daliyBasicHistroyService
									.queryListByCodeForModel(code, date, queryPage250).getContent();
							LineVol lineVol = new LineVol(dailyList);
							// 缩量
							if (vb == 0 || (lineVol.isShortVolThanYerteryDay() && lineVol.isShortVolV2(vb))) {// 2.没有超过5天均量1.3倍,今天的量比昨天高
								LinePrice linePrice = new LinePrice(code, date, daliyTradeHistroyService);

								boolean b6 = linePrice.checkPriceBack6dayWhitTodayV2();// 5.回调过超10%
//								boolean b5 = linePrice.check3dayPriceV2();// 6.对比3天-价
								boolean b4 = linePrice.isLowClosePriceToday(d2);
								if (b6 && !b4) {
									LineAvgPrice lineAvg = new LineAvgPrice(code, date, avgService, dailyList);
									if (lineAvg.feedData()) {
										if (lineAvg.isWhiteHorseV2()) {
											if (linePrice.oneYearCheck(oneYear, code, date)) {
												saveOkRec(code, date, day, codesamples, d2);
											}
										}
									}
								}
							}
						}

					} catch (Exception e) {
						ErrorLogFileUitl.writeError(e, s.getCode(), code, date + "");
					}
				}
			}
			samples.addAll(codesamples);
			// ThreadsUtil.sleepSleep1Seconds();
		}

		log.info("V3获取样本数:" + samples.size());

		int total_all = samples.size();
		if (total_all > 0) {
			TraceSortv2StatVo stat = new TraceSortv2StatVo();
			String filepath = FILE_FOLDER + "v3" + startDate + "_" + endDate + "_" + d + "_" + oneYear + "_" + vb + "_"
					+ batch + ".log";
			stat(filepath, stat, samples);
			sendMessge("v3", batch, startDate, endDate, oneYear, d, vb, stat, total_all, sysstart);
		} else {
			WxPushUtil.pushSystem1("v3样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb + ",一年涨幅限制"
					+ oneYear + ")样本数量:" + total_all);
		}

	}

	/**
	 * 右侧交易
	 */
	public void sortv2(String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
		try {
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
			if (StringUtils.isBlank(startDate)) {
				startDate = "20200101";
			}
			if (StringUtils.isBlank(endDate)) {
				endDate = DateUtil.getTodayYYYYMMDD();
			}
			log.info("startDate={},endDate={}", startDate, endDate);
			try {
				boolean getLock = sempv2.tryAcquire(1, TimeUnit.HOURS);
				if (!getLock) {
					log.warn("No Locked");
					return;
				}
				log.info("Get Locked");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			int[] days = { 2, 3 };
			double[] volBases = { 0.0, 1.2, 1.3 };
			int[] oneYearups = { 1 };// 1年未大涨的（TODO,第一波是否涨超30%？）
			for (int oneYear : oneYearups) {
				for (double vb : volBases) {
					for (int d : days) {
						try {
							v2Really(startDate, endDate, codelist, d, oneYear, vb, sysstart, batch);
						} catch (Exception e) {
							e.printStackTrace();
							WxPushUtil.pushSystem1("v2样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb
									+ ",一年涨幅限制" + oneYear + ")样本出错");
						}
					} // for-days
				} // for volbases
			} // for-year
			WxPushUtil.pushSystem1("v2样本完成！" + startDate + " " + endDate);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("v2样本出错！" + startDate + " " + endDate);
		} finally {
			sempv2.release();
		}
	}

	// 一般涨幅，
	public void v2Really(String startDate, String endDate, List<StockBaseInfo> codelist, int d, int oneYear, double vb,
			String sysstart, int batch) {

		int day = d + 1;

		List<TraceSortv2Vo> samples = new LinkedList<TraceSortv2Vo>();
		// start..
		for (StockBaseInfo s : codelist) {
			StockAType sa = StockAType.formatCode(s.getCode());
			if (StockAType.KCB == sa) {
				continue;
			}
			String code = s.getCode();
			boolean onlineYear = stockBasicService.online1Year(code);
			if (!onlineYear) {
				// log.info("{},Online 上市不足1年", code);
				continue;
			}
			List<TraceSortv2Vo> codesamples = new LinkedList<TraceSortv2Vo>();

			JSONArray array2 = tushareSpider.getStockDaliyTrade(s.getTs_code(), null, startDate, endDate);
			if (array2 != null && array2.size() > 0) {
				for (int ij = (array2.size() - 1); ij >= 0; ij--) {
					DaliyBasicInfo d2 = new DaliyBasicInfo();
					d2.setCode(code);
					d2.daily(array2.getJSONArray(ij));
					int date = d2.getTrade_date();
					log.info("code={},date={}", code, date);
					try {
						// 1.涨幅检查：上涨且位未超过8%
						if (d2.getTodayChangeRate() > 0 && d2.getTodayChangeRate() <= 8.0) {
							List<DaliyBasicInfo> dailyList = daliyBasicHistroyService
									.queryListByCodeForModel(code, date, queryPage250).getContent();

							LineVol lineVol = new LineVol(dailyList);
							// 量级检查:缩量
							if (vb == 0 || (lineVol.isShortVolThanYerteryDay() && lineVol.isShortVolV2(vb))) {// 2.没有超过5天均量1.3倍,今天的量比昨天高
								LinePrice linePrice = new LinePrice(code, date, daliyTradeHistroyService);

								boolean b6 = linePrice.checkPriceBack6dayWhitTodayV2();// 5.回调过超10%
								boolean b5 = linePrice.check3dayPriceV2();// 6.对比3天-价
								boolean b4 = linePrice.isLowClosePriceToday(d2);// 上影线
								// 价格检查
								if (b6 && b5 && !b4) {
									LineAvgPrice lineAvg = new LineAvgPrice(code, date, avgService, dailyList);
									// 均线
									if (lineAvg.feedData()) {
										if (lineAvg.isWhiteHorseV2()) {
											DaliyBasicInfo today = d2;
											StockAvg av = lineAvg.todayAv;
											// 3.一阳穿N线
											if ((av.getAvgPriceIndex3() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex5() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex10() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex20() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex30() > today.getYesterdayPrice()//
											)// 4.昨日收盘价在任意均线之下
													&& (today.getClose() > av.getAvgPriceIndex3()
															&& today.getClose() > av.getAvgPriceIndex5()
															&& today.getClose() > av.getAvgPriceIndex10()
															&& today.getClose() > av.getAvgPriceIndex20()
															&& today.getClose() > av.getAvgPriceIndex30()//
													)//
											) {
												if (linePrice.oneYearCheck(oneYear, code, date)) {
													saveOkRec(code, date, day, codesamples, d2);
												}
											}
										}
									}

								}

							}
						}

					} catch (Exception e) {
						ErrorLogFileUitl.writeError(e, s.getCode(), code, date + "");
					}
				}
			} else {
				log.info("V2获取样本{} 未获得数据", code);
			}
			samples.addAll(codesamples);
			// ThreadsUtil.thsSleepRandom();
		}

		log.info("V2获取样本数:{}", samples.size());

		int total_all = samples.size();
		if (total_all > 0) {
			TraceSortv2StatVo stat = new TraceSortv2StatVo();
			String filepath = FILE_FOLDER + "v2" + startDate + "_" + endDate + "_" + d + "_" + oneYear + "_" + vb + "_"
					+ batch + ".log";
			stat(filepath, stat, samples);
			sendMessge("v2", batch, startDate, endDate, oneYear, d, vb, stat, total_all, sysstart);
		} else {
			WxPushUtil.pushSystem1("v2样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb + ",一年涨幅限制"
					+ oneYear + ")样本数量:" + total_all);
		}
	}

	private void saveOkRec(String code, int date, int day, List<TraceSortv2Vo> codesamples, DaliyBasicInfo d2) {
		if (codesamples.size() > 0) {
			TraceSortv2Vo tsv = codesamples.get(codesamples.size() - 1);
			// 30天之前
			int day30 = Integer.valueOf(DateUtil.formatYYYYMMDD(DateUtil.addDate(date + "", -day)));
			if (day30 > tsv.getDate()) {// 30天之内存在则不能在作为样本
			} else {
				// 忽略
				return;
			}
		}
		try {
			EsQueryPageReq queryPage6 = new EsQueryPageReq(day);
			TraceSortv2Vo t1 = new TraceSortv2Vo();
			t1.setCode(code);
			t1.setDate(date);
			//
			List<TradeHistInfoDaliy> dailyList2 = new ArrayList<TradeHistInfoDaliy>();
			List<TradeHistInfoDaliy> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastQfq(t1.getCode(),
					t1.getDate(), 0, queryPage6, SortOrder.ASC);// 返回的list是不可修改对象
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
			FinYjkb ckb = financeService.getLastFinYjkbReport(code, date, currYear, currJidu);
			if (ckb == null) {
				FinYjyg cyg = financeService.getLastFinYjygReport(code, date, currYear, currJidu);
				if (cyg == null) {
					// 财务季度的最新快预告
					int finYear = DateUtil.getYear(fin.getDate());
					int finJidu = DateUtil.getJidu(fin.getDate());
					FinYjkb prekb = financeService.getLastFinYjkbReport(code, date, finYear, finJidu);
					if (prekb == null) {
						FinYjyg precyg = financeService.getLastFinYjygReport(code, date, finYear, finJidu);
						if (precyg != null) {
							t1.setYg(precyg);
						}
					} else {
						t1.setKb(prekb);
					}
				} else {
					t1.setYg(cyg);
				}
			} else {
				t1.setKb(ckb);
			}

			// 最新快报是否当前季度？最新预告是否当前季度？最新快报财报当前季度？//TODO清缓存

			codesamples.add(t1);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, code, date + "", "");
			e.printStackTrace();
		}
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
			LogFileUitl.writeLog(filepath, sb.toString());
		}
	}

	private void sendMessge(String vers, int batch, String startDate, String endDate, int oneYear, int d, double vb,
			TraceSortv2StatVo stat, int total_all, String sysstart) {

		HistTrace ht = new HistTrace();
		ht.setBatch(batch);
		ht.setStartDate(Integer.valueOf(startDate));
		ht.setEndDate(Integer.valueOf(endDate));
		ht.setVersion(vers);
		ht.setOneYear(oneYear);
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

		WxPushUtil.pushSystem1(vers + " 样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb + ",一年涨幅限制"
				+ oneYear + ")样本数量:" + total_all//
				+ ",[理论最高盈利]次数:" + stat.getCnt_up() + ",盈利概率:"
				+ CurrencyUitl.roundHalfUpWhithPercent(stat.getCnt_up() / Double.valueOf(total_all)) + "%" + ",总盈利百分比:"
				+ CurrencyUitl.roundHalfUp(stat.getTotalProfit()) //
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
