package com.stable.service.trace;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.enums.StockAType;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LineVol;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.RedisUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.retrace.TraceSortv1Vo;
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
	private TickDataService tickDataService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public static final Semaphore semp = new Semaphore(1);

	EsQueryPageReq queryPage250 = new EsQueryPageReq(250);
	EsQueryPageReq queryPage20 = new EsQueryPageReq(20);
	EsQueryPageReq queryPage5 = new EsQueryPageReq(5);

	/**
	 * 右侧交易
	 */
	public void sortv2(String startDate, String endDate) {
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
				boolean getLock = semp.tryAcquire(1, TimeUnit.HOURS);
				if (!getLock) {
					log.warn("No Locked");
					return;
				}
				log.info("Get Locked");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			List<TraceSortv2Vo> samples = new LinkedList<TraceSortv2Vo>();
			for (StockBaseInfo s : codelist) {
				StockAType sa = StockAType.formatCode(s.getCode());
				if (StockAType.KCB == sa) {
					continue;
				}
				List<TraceSortv2Vo> codesamples = new LinkedList<TraceSortv2Vo>();
				String code = s.getCode();
				// 最后除权日期
				int lastDividendDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
				// code均线 (qfq)
				List<StockAvg> avgSaveList = Collections.synchronizedList(new LinkedList<StockAvg>());

				JSONArray array2 = tushareSpider.getStockDaliyTrade(s.getTs_code(), null, startDate, endDate);
				if (array2 != null && array2.size() > 0) {
					for (int ij = 0; ij < array2.size(); ij++) {
						DaliyBasicInfo d2 = new DaliyBasicInfo();
						d2.setCode(code);
						d2.daily(array2.getJSONArray(ij));
						int date = d2.getTrade_date();
						log.info("code={},date={}", code, date);
						try {
							// 上涨
							if (d2.getTodayChangeRate() > 0) {
								List<DaliyBasicInfo> dailyList = daliyBasicHistroyService
										.queryListByCodeForModel(code, date, queryPage250).getContent();

								LineVol lineVol = new LineVol(dailyList);
								// 缩量
								if (lineVol.isShortVolV2()) {
									LineAvgPrice lineAvg = new LineAvgPrice(code, date, avgService, lastDividendDate,
											avgSaveList, dailyList);
									if (lineAvg.feedData()) {
										if (lineAvg.isWhiteHorseV2()) {
											DaliyBasicInfo today = d2;
											StockAvg av = lineAvg.todayAv;
											// 一阳穿N线
											if ((av.getAvgPriceIndex3() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex5() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex10() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex20() > today.getYesterdayPrice()
													|| av.getAvgPriceIndex30() > today.getYesterdayPrice()//
											)// 昨日收盘价在任意均线之下
													&& (today.getClose() > av.getAvgPriceIndex3()
															&& today.getClose() > av.getAvgPriceIndex5()
															&& today.getClose() > av.getAvgPriceIndex10()
															&& today.getClose() > av.getAvgPriceIndex20()
															&& today.getClose() > av.getAvgPriceIndex30()//
													)//
											) {
												LinePrice linePrice = new LinePrice(code, date,
														daliyTradeHistroyService);

												boolean b6 = linePrice.checkPriceBack6dayWhitoutTodayV2();// 回调过超10%
												boolean b5 = linePrice.check3dayPriceV2();// 对比3天-价

												if (b6 && b5) {
													if (codesamples.size() > 0) {
														TraceSortv2Vo tsv = codesamples.get(codesamples.size() - 1);
														// 30天之前
														int day30 = Integer.valueOf(DateUtil
																.formatYYYYMMDD(DateUtil.addDate(date + "", -30)));
														if (day30 > tsv.getDate()) {// 30天之内存在则不能在作为样本
															TraceSortv2Vo tv = new TraceSortv2Vo();
															tv.setCode(code);
															tv.setDate(date);
															codesamples.add(tv);
														} else {
															// 忽略
														}
													} else {
														TraceSortv2Vo tv = new TraceSortv2Vo();
														tv.setCode(code);
														tv.setDate(date);
														codesamples.add(tv);
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
				}

				if (avgSaveList.size() > 0) {
					avgService.saveStockAvg(avgSaveList);
				}
				samples.addAll(codesamples);
				ThreadsUtil.thsSleepRandom();
			}

			log.info("V2获取样本数:" + samples.size());
			for (TraceSortv2Vo t1 : samples) {
				try {
					List<TradeHistInfoDaliy> dailyList = daliyTradeHistroyService.queryListByCode(t1.getCode(),
							t1.getDate(), 0, queryPage20, SortOrder.ASC);
					// i=0;是涨停当天
					TradeHistInfoDaliy d0 = dailyList.get(0);
					double max = dailyList.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed)).get()
							.getClosed();// 一个月
					if (max > d0.getClosed()) {
						t1.setOk(true);
					}
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, t1.getCode(), t1.getDate() + "", "");
					e.printStackTrace();
				}
			}

			int total1 = 0;
			int isok1 = 0;
			// 全部
			log.info("ALL samples....");
			for (TraceSortv2Vo t1 : samples) {
				total1++;
				if (t1.isOk()) {
					isok1++;
				}
				log.info(t1.toDetailStr());
			}

			if (total1 > 0) {
				WxPushUtil.pushSystem1(startDate + " " + endDate + "样本数量:" + samples.size() + ",[所有]成功买入次数:" + total1
						+ ",盈利次数:" + isok1 + ",盈利:" + CurrencyUitl.roundHalfUp(isok1 / Double.valueOf(total1)) + "%");
			} else {
				WxPushUtil.pushSystem1(startDate + " " + endDate + "样本数量:" + samples.size() + ",无成功买入样例");
			}
		} catch (

		Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(startDate + " " + endDate + "样本出错！");
		}
	}

	public void sortv1(String startDate, String endDate) {
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
				boolean getLock = semp.tryAcquire(1, TimeUnit.HOURS);
				if (!getLock) {
					log.warn("No Locked");
					return;
				}
				log.info("Get Locked");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			List<TraceSortv1Vo> samples = new LinkedList<TraceSortv1Vo>();
			for (StockBaseInfo s : codelist) {
				StockAType sa = StockAType.formatCode(s.getCode());
				if (StockAType.KCB == sa) {
					continue;
				}
				try {
					JSONArray array2 = tushareSpider.getStockDaliyTrade(s.getTs_code(), null, startDate, endDate);
					if (array2 != null && array2.size() > 0) {
						for (int ij = 0; ij < array2.size(); ij++) {
							DaliyBasicInfo d2 = new DaliyBasicInfo();
							d2.setCode(s.getCode());
							d2.daily(array2.getJSONArray(ij));
							TraceSortv1Vo tv = tickDataService.check(d2.getCode(), null, d2);
							if (tv != null) {
								samples.add(tv);
							}
						}
					}

				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, s.getCode(), startDate, startDate);
				}
				ThreadsUtil.thsSleepRandom();
			}

			log.info("获取样本数:" + samples.size());
			for (TraceSortv1Vo t1 : samples) {
				try {
					List<DaliyBasicInfo> dailyList = daliyBasicHistroyService.queryListByCode(
							t1.getDaliyBasicInfo().getCode(), t1.getDaliyBasicInfo().getTrade_date(), 0, queryPage20,
							SortOrder.ASC);
					// i=0;是涨停当天
					DaliyBasicInfo d1 = dailyList.get(1);// 第二天
					double topPrice = CurrencyUitl.topPrice(d1.getYesterdayPrice(), false);
					if (topPrice > d1.getOpen()) {
						t1.setBuyed(true);
//						DaliyBasicInfo d2 = dailyList.get(2);// 第三天开盘价或者收盘价>第二天集合竞价的买入价
						double max = dailyList.stream().max(Comparator.comparingDouble(DaliyBasicInfo::getClose)).get()
								.getClose();// 一个月
						if (max > d1.getOpen()) {
							t1.setOk(true);
						}
					}
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, t1.getDaliyBasicInfo().getCode(),
							t1.getDaliyBasicInfo().getTrade_date() + "", "");
					e.printStackTrace();
				}
			}

			int total1 = 0;
			int isok1 = 0;
			// 全部
			log.info("ALL samples....");
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed()) {
					total1++;
					if (t1.isOk()) {
						isok1++;
					}
					log.info(t1.toDetailStr());
				}
			}
			// 全部-缩量
			int totalsyes = 0;
			int isoksyes = 0;
			log.info("ShortVol....");
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed() && t1.isShortVol()) {
					totalsyes++;
					if (t1.isOk()) {
						isoksyes++;
					}
					log.info(t1.toDetailStr());
				}
			}
			// 全部-放量
			int totalsno = 0;
			int isoksno = 0;
			log.info("ShortVolNo....");
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed() && !t1.isShortVol()) {
					totalsno++;
					if (t1.isOk()) {
						isoksno++;
					}
					log.info(t1.toDetailStr());
				}
			}
			// 白马-缩量
			int totalwhsy = 0;
			int isokwhsy = 0;
			log.info("WhiteHorse-sort....");
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed() && t1.isWhiteHorse() && t1.isShortVol()) {
					totalwhsy++;
					if (t1.isOk()) {
						isokwhsy++;
					}
					log.info(t1.toDetailStr());
				}
			}
			// 白马-放量
			int totalwhsn = 0;
			int isokwhsn = 0;
			log.info("WhiteHorse-sortno....");
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed() && t1.isWhiteHorse() && !t1.isShortVol()) {
					totalwhsn++;
					if (t1.isOk()) {
						isokwhsn++;
					}
					log.info(t1.toDetailStr());
				}
			}
			// 白马
			int total2 = 0;
			int isok2 = 0;
			log.info("WhiteHorse....");
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed() && t1.isWhiteHorse()) {
					total2++;
					if (t1.isOk()) {
						isok2++;
					}
					log.info(t1.toDetailStr());
				}
			}

			if (total1 > 0) {
				WxPushUtil.pushSystem1(startDate + " " + endDate + "样本数量:" + samples.size() + ",[所有]成功买入次数:" + total1
						+ ",盈利次数:" + isok1 + ",盈利:" + CurrencyUitl.roundHalfUp(isok1 / Double.valueOf(total1)) + "%"
						+ ",[所有-放量]成功买入次数:" + totalsno + ",盈利次数:" + isoksno + ",盈利:"
						+ CurrencyUitl.roundHalfUp(isoksno / Double.valueOf(totalsno)) + "%" + ",[所有-缩量]成功买入次数:"
						+ totalsyes + ",盈利次数:" + isoksyes + ",盈利:"
						+ CurrencyUitl.roundHalfUp(isoksyes / Double.valueOf(totalsyes)) + "%" + ",[白马]成功买入次数:" + total2
						+ ",盈利次数:" + isok2 + ",盈利:" + CurrencyUitl.roundHalfUp(isok2 / Double.valueOf(total2)) + "%"
						+ ",[白马-放量]成功买入次数:" + totalwhsn + ",盈利次数:" + isokwhsn + ",盈利:"
						+ CurrencyUitl.roundHalfUp(isokwhsn / Double.valueOf(totalwhsn)) + "%" + ",[白马-缩量]成功买入次数:"
						+ totalwhsy + ",盈利次数:" + isokwhsy + ",盈利:"
						+ CurrencyUitl.roundHalfUp(isokwhsy / Double.valueOf(totalwhsy)) + "%");
			} else {
				WxPushUtil.pushSystem1(startDate + " " + endDate + "样本数量:" + samples.size() + ",无成功买入样例");
			}
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(startDate + " " + endDate + "样本出错！");
		}
	}
}
