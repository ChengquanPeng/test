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
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.StockAType;
import com.stable.es.dao.base.EsHistTraceDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
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
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
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
	@Autowired
	private TradeCalService tradeCalService;

	private String FILE_FOLDER = "/my/free/pvhtml/";

	public static final Semaphore sempAll = new Semaphore(1);

	public void sortv1(double min, double max, String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		if (StringUtils.isBlank(startDate)) {
			startDate = "20200101";
		}
		if (min <= 0) {
			min = 3.5;
		}
		if (max <= 0) {
			max = 5;
		}
		if (StringUtils.isBlank(endDate)) {
			endDate = DateUtil.getTodayYYYYMMDD();
			endDate = tradeCalService.getPretradeDate(endDate);
			endDate = tradeCalService.getPretradeDate(endDate);
		}
		int sd = Integer.valueOf(startDate);
		int ed = Integer.valueOf(endDate);
		String version = "v1";
		String other = "min=" + min + ",max=" + max;
		log.info("startDate={},endDate={},{}", startDate, endDate, other);
		try {
			boolean getLock = sempAll.tryAcquire(10, TimeUnit.HOURS);
			if (!getLock) {
				log.warn("sort V1 No Locked");
				return;
			}
			log.info("sort V1 Get Locked");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		try {
			double minRate = min;
			double maxRate = max;
			double[] volBases = { 0, 1.2, 1.3, 2 };
			int[] openPrice = { 0, 1, 2 };// 0:不管高低开，1高开，2低开
			int[] marketPrice = { 0, 1, 2 };// 0:不管大盘，1上涨，2下跌
			int day = 1;
//			int[] oneYearups = { 1 };// 1年未大涨的（TODO,第一波是否涨超30%？）
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
			log.info("codelist:" + codelist.size());
			for (int op : openPrice) {
				for (int mp : marketPrice) {
					for (double vb : volBases) {
						String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
						String detailOther = other + ",openPrice=" + op + ",marketPrice=" + mp;
						try {
							List<TraceSortv2Vo> samples = Collections.synchronizedList(new LinkedList<TraceSortv2Vo>());
							// start..
							CountDownLatch cnt = new CountDownLatch(codelist.size());
							for (StockBaseInfo ss : codelist) {
								String code = ss.getCode();
								if (!stockBasicService.online2YearChk(code, ed)) {
									log.info("{} 上市不足2年", ss.getCode());
									cnt.countDown();
									continue;
								}
								TasksWorkerModel.add(code, new TasksWorkerModelRunnable() {
									@Override
									public void running() {
										try {
											int twoYearChkDate = DateUtil
													.getNext2Year(Integer.valueOf(ss.getList_date()));
											// log.info("code={},date={}", code, date);
											List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService
													.queryListByCodeWithLastNofq(code, sd, ed,
															EsQueryPageUtil.queryPage9999, SortOrder.ASC);// 返回的list是不可修改对象
											if (dailyList0 == null || dailyList0.size() <= 0) {
												return;
											}
											double yesterRate = 99;
											for (TradeHistInfoDaliyNofq td : dailyList0) {
												if (td.getDate() < twoYearChkDate) {
													continue;
												}
												int date = td.getDate();
												double todayChangeRate = td.getTodayChangeRate();
												try {
													boolean b1 = yesterRate < todayChangeRate;// 今日涨幅超过昨天;
													if (todayChangeRate >= minRate && todayChangeRate <= maxRate // 1.上涨在区间
															&& b1// 今日涨幅超过昨天;
															&& strongService.checkMarketPrice(mp, code, date)// 大盘涨跌
															&& checkOpenPrice(op, td)) {// 高低开
														LinePrice linePrice = new LinePrice(code, date,
																daliyTradeHistroyService);
														boolean b4 = linePrice.isLowClosePriceToday(todayChangeRate,
																td.getYesterdayPrice(), td.getClosed(), td.getHigh());// 排除上影线
														if (!b4) {
															// 是否检查量(vb==0).或缩量(),或放量(vb==2)
															boolean volOk = false;
															if (vb == 0) {
																volOk = true;
															} else {
																LineVol lineVol = new LineVol(code, date,
																		daliyTradeHistroyService);
																volOk = (vb != 2 && lineVol.isShortVol(vb)
																		|| (vb == 2 && (lineVol.isHighVol())));
															}
															if (volOk) {
																if (linePrice.oneYearCheck(code, date)) {
																	LineAvgPrice lineAvg = new LineAvgPrice(avgService);
																	if (lineAvg.isWhiteHorseV2(code, date)) {
																		TraceSortv2Vo t1 = saveOkRecv1(code, date);
																		if (t1 != null) {
																			samples.add(t1);
																		}
																	}
																}
															}
														}
													}
												} catch (Exception e) {
													e.printStackTrace();
													ErrorLogFileUitl.writeError(e, code, date + "", "");
												}
												yesterRate = todayChangeRate;
											}

										} catch (Exception e) {
											ErrorLogFileUitl.writeError(e, code, sd + "", ed + "");
											e.printStackTrace();
										} finally {
											cnt.countDown();
										}
									}
								});
							}
							try {
								cnt.await();
							} catch (InterruptedException ex) {
								ex.printStackTrace();
							}
							int total_all = samples.size();
							log.info(version + " 获取样本数:" + total_all);
							if (total_all > 0) {
								TraceSortv2StatVo stat = new TraceSortv2StatVo();
								String filepath = FILE_FOLDER + version + "_" + startDate + "_" + ed + "_" + day + "_"
										+ +vb + "_" + batch;
								stat(filepath, stat, samples);
								sendMessge(version, batch, startDate, ed + "", day, vb, stat, total_all, sysstart,
										detailOther, op, mp);
							} else {
								WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + "条件(" + day + "天期,交易量：" + vb
										+ ",开盘价：" + op + ",大盘涨跌:" + mp + " 获取样本数为0");
							}

						} catch (Exception e) {
							e.printStackTrace();
							WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + "条件(" + day + "天期,交易量：" + vb
									+ ",开盘价：" + op + ",大盘涨跌:" + mp + ")样本出错");
						}
					} // for-volbase
				} // for-marketPrice
			} // for-openPrice
			WxPushUtil.pushSystem1(version + "样本完成！" + sd + " " + ed);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(version + "样本出错！" + sd + " " + ed);
		} finally {
			sempAll.release();
		}
	}

	public void sortv2(String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		if (StringUtils.isBlank(startDate)) {
			startDate = "20200101";
		}
		if (StringUtils.isBlank(endDate)) {
			endDate = DateUtil.getTodayYYYYMMDD();
		}
		log.info("startDate={},endDate={}", startDate, endDate);
		try {
			boolean getLock = sempAll.tryAcquire(5, TimeUnit.HOURS);
			if (!getLock) {
				log.warn("Sort V2 No Locked");
				return;
			}
			log.info("Sort V2 Get Locked");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		String sd = startDate;
		String ed = endDate;
		try {
			String[] versions = { "v2", "v3" };
			int[] days = { 2, 3 };
			double[] volBases = { 0, 1.2, 1.3, 2 };
//					int[] oneYearups = { 1 };// 1年未大涨的（TODO,第一波是否涨超30%？）
			for (String ver : versions) {
				for (double vb : volBases) {
					for (int day : days) {
						try {
							reallymodelForJob(ver, sd, ed, day, vb, batch);
							ThreadsUtil.sleepRandomSecBetween15And30();
						} catch (Exception e) {
							e.printStackTrace();
							WxPushUtil.pushSystem1(
									versions + "样本区间:" + sd + " " + ed + "条件(" + day + "天期,交易量" + vb + ")样本出错");
						}
					} // for-days
				} // for-volbase
			} // versions
			WxPushUtil.pushSystem1("V2 样本完成！" + sd + " " + ed);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("V2 样本出错！" + sd + " " + ed);
		} finally {
			sempAll.release();
		}
	}

	private TraceSortv2Vo runinner(String code, int date, double min, double max, int day, double vb, boolean isV2,
			double todayChangeRate, double yesterdayPrice, double closedPrice, double highPrice) {
		try {
			// log.info("code={},date={}", code, date);
			// 1.上涨且未超过8%
			if (todayChangeRate >= min && todayChangeRate <= max) {

				// 是否检查量(vb==0).或缩量(),或放量(vb==2)
				boolean volOk = false;
				if (vb == 0) {
					volOk = true;
				} else {
					// && lineVol.isShortVolThanYerteryDay()
					LineVol lineVol = new LineVol(code, date, daliyTradeHistroyService);
					volOk = (vb != 2 && lineVol.isShortVol(vb) || (vb == 2 && (lineVol.isHighVol())));
				}
				if (volOk) {
					LinePrice linePrice = new LinePrice(code, date, daliyTradeHistroyService);
					boolean b6 = linePrice.checkPriceBack6dayWhitTodayV2();// 5.回调过超10%
//					boolean b5 = linePrice.check3dayPriceV2();// 6.对比3天-价
					boolean b4 = linePrice.isLowClosePriceToday(todayChangeRate, yesterdayPrice, closedPrice,
							highPrice);// 上影线
					if (b6 && !b4 && (isV2 ? linePrice.check3dayPriceV2() : true)
							&& linePrice.oneYearCheck(code, date)) {
						LineAvgPrice lineAvg = new LineAvgPrice(avgService);
						if (lineAvg.isWhiteHorseV2(code, date)) {
							if (isUpNline(isV2, lineAvg, yesterdayPrice, closedPrice)) {
								return saveOkRec(code, date, day);
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

	public synchronized void reallymodelForJob(String ver, String startDate, String endDate, int day, double vb,
			int batch) {
		reallymodelForRealTime(ver, startDate, endDate, day, vb, batch);
	}

	public void reallymodelForRealTime(String ver, String startDate, String endDate, int day, double vb, int batch) {
		String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
		boolean isV2 = "v2".equals(ver);
		String version = (isV2 ? "v2" : "v3");
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
		String other = "min=" + min + ",max=" + max;
		int oneYearChkDate = Integer.valueOf(startDate);
		// start..
		log.info("codelist:" + codelist.size());
		log.info("version={},day={},vb={},startDate={},endDate={}", version, day, vb, startDate, endDate);
		CountDownLatch cnt = new CountDownLatch(codelist.size());
		for (StockBaseInfo s : codelist) {
			StockAType sa = StockAType.formatCode(s.getCode());
			if (StockAType.KCB == sa) {
				cnt.countDown();
				continue;
			}
			String code = s.getCode();
			if (!stockBasicService.online1YearChk(code, oneYearChkDate)) {
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
//									if (checkExist(codesamples, d2.getTrade_date(), day)) {
//										continue;
//									}
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
		int total_all = samples.size();
		log.info(version + "获取样本数:" + total_all);
		if (total_all > 0) {
			TraceSortv2StatVo stat = new TraceSortv2StatVo();
			String filepath = FILE_FOLDER + version + "_" + startDate + "_" + endDate + "_" + day + "_" + +vb + "_"
					+ batch;
			stat(filepath, stat, samples);
			sendMessge(version, batch, startDate, endDate, day, vb, stat, total_all, sysstart, other, 0, 0);
		} else {
			WxPushUtil.pushSystem1(
					version + "样本区间:" + startDate + " " + endDate + "条件(" + day + "天期,交易量" + vb + ")样本数量:0");
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

//	private boolean checkExist(List<TraceSortv2Vo> codesamples, int date, int day) {
//		if (codesamples != null && codesamples.size() > 0) {
//			TraceSortv2Vo tsv = codesamples.get(codesamples.size() - 1);
//			// tsv.getDate()：目前最后的日期
//			// tsv.getDate()+day：相加后的日期
//			// 相加后的日期：在这日期之前不允许存在
//			int day30 = Integer.valueOf(DateUtil.formatYYYYMMDD(DateUtil.addDate(tsv.getDate() + "", day)));
//			if (day30 > date) {// 30天之内存在则不能在作为样本
//				return true;
//			}
//		}
//		return false;
//	}

	private TraceSortv2Vo saveOkRecv1(String code, int date) {
		try {
			TraceSortv2Vo t1 = new TraceSortv2Vo();
			t1.setCode(code);
			t1.setDate(date);
			t1.setMarketIndex(strongService.getIndexPrice(code, date));
			//
			// date=已收盘的日期
			List<TradeHistInfoDaliy> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, date, 0,
					EsQueryPageUtil.queryPage2, SortOrder.ASC);// 返回的list是不可修改对象
			// i=0;是当天
			TradeHistInfoDaliy d0 = dailyList0.get(0);
			TradeHistInfoDaliy d1 = dailyList0.get(1);
			t1.setMinLowPrice(d1.getClosed());// 最低价格
			t1.setBuyPrice(d0.getClosed());
			t1.setMaxPrice(d1.getClosed());// 最高盈利
			t1.setMinPrice(d1.getClosed());// 最高亏损
			t1.setSellPrice(d1.getClosed());
			t1.setBuyDayRate(d0.getTodayChangeRate());
			DaliyBasicInfo basic = daliyBasicHistroyService.getDaliyBasicInfoByDate(code, date);
			t1.setBasic(basic);

			// 最新财务
			FinanceBaseInfo fin = financeService.getLastFinaceReport(code, date);
			t1.setFin(fin);
			// 最新快报&预告
			int currYear = DateUtil.getYear(date);
			int currJidu = DateUtil.getJidu(date);
			// 当前季度的快预报（当前季度中后期）
			if (fin != null && date > 20200101 && !getLastKygb(t1, code, date, currYear, currJidu)) {
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

	private TraceSortv2Vo saveOkRec(String code, int date, int day) {
		try {
			TraceSortv2Vo t1 = new TraceSortv2Vo();
			t1.setCode(code);
			t1.setDate(date);
			t1.setMarketIndex(strongService.getIndexPrice(code, date));
			//
			List<TradeHistInfoDaliy> dailyList2 = new ArrayList<TradeHistInfoDaliy>();
			// date=已收盘的日期
			EsQueryPageReq queryPage6 = new EsQueryPageReq(day + 1);// 为啥+1:queryListByCodeWithLastQfq 包含当天了。
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
			DaliyBasicInfo basic = daliyBasicHistroyService.getDaliyBasicInfoByDate(code, date);
			t1.setBasic(basic);

			// 最新财务
			FinanceBaseInfo fin = financeService.getLastFinaceReport(code, date);
			t1.setFin(fin);
			// 最新快报&预告
			int currYear = DateUtil.getYear(date);
			int currJidu = DateUtil.getJidu(date);
			// 当前季度的快预报（当前季度中后期）
			if (fin != null && date > 20200101 && !getLastKygb(t1, code, date, currYear, currJidu)) {
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

	private String title = "代码,日期,当日涨幅,当日大盘涨幅,买入价,卖出价,是否盈利,盈利多少,最高收盘价,最低收盘价,最低价幅度,最低价,最低价幅度,市盈率(静),市盈率(TTM),"
			+ "流通市值(万元),量比,最近财务快预告日期,营业同比增长,净利同比增长,最新财务公告日期,营业同比增长,净利同比增长,";

	private void stat(String filepath, TraceSortv2StatVo stat, List<TraceSortv2Vo> samples) {
		// 日期排序
		samples.sort(new Comparator<TraceSortv2Vo>() {
			public int compare(TraceSortv2Vo o1, TraceSortv2Vo o2) {
				return o1.getDate() - o2.getDate();
			}
		});
		StringBuffer sb = new StringBuffer();
		sb.append(title).append(FileWriteUitl.LINE_FILE);
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
			TraceSortv2StatVo stat, int total_all, String sysstart, String other, int op, int mp) {

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
		ht.setAct_percent(CurrencyUitl.roundHalfUpWhithPercent(stat.getAct_cnt_up() / Double.valueOf(total_all)));
		ht.setLossSettAct(stat.getStatLossAct());
		ht.setLossSettClosedPrice(stat.getStatLossLowClosedPrice());
		ht.setLossSettLowPrice(stat.getStatLossLowPrice());
		ht.setId(UUID.randomUUID().toString());
		ht.setOther(other);
		esHistTraceDao.save(ht);

		WxPushUtil.pushSystem1(vers + " 样本区间:" + startDate + " " + endDate + "条件(" + (d) + "天期,交易量" + vb + ",高低开:" + op
				+ ",大盘涨跌:" + mp + ")样本数量:" + total_all//
				+ ",[理论最高盈利]次数:" + stat.getCnt_up() + ",盈利概率:"
				+ CurrencyUitl.roundHalfUpWhithPercent(stat.getCnt_up() / Double.valueOf(total_all)) + "%" + ",总盈利百分比:"
				+ CurrencyUitl.roundHalfUp(stat.getTotalProfit()) //
				+ "%,[理论最高亏损]次数:" + stat.getCnt_down() + ",亏损概率:"
				+ CurrencyUitl.roundHalfUpWhithPercent(stat.getCnt_down() / Double.valueOf(total_all)) + "%"
				+ ",总亏损百分比:" + CurrencyUitl.roundHalfUp(stat.getTotalLoss()) //
				+ "%,[实际盈利]次数:" + stat.getAct_cnt_up() + ",盈利概率:" + ht.getAct_percent() + "%" + ",实际盈亏百分比:"
				+ CurrencyUitl.roundHalfUp(stat.getAct_totalProfit()) + "%" //
				// 盈利
				+ "[实际亏损分布]" + ht.getLossSettAct() //
				+ "[收盘价亏损分布]" + ht.getLossSettClosedPrice() //
				+ "[最低价亏损分布]" + ht.getLossSettLowPrice() //
				// 亏损
				+ ",开始时间:" + sysstart//
		);
	}

	public void middle(String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		if (StringUtils.isBlank(startDate)) {
			startDate = "20200101";
		}
		if (StringUtils.isBlank(endDate)) {
			endDate = DateUtil.getTodayYYYYMMDD();
			endDate = tradeCalService.getPretradeDate(endDate);
			endDate = tradeCalService.getPretradeDate(endDate);
		}
		int sd = Integer.valueOf(startDate);
		int ed = Integer.valueOf(endDate);
		String version = "middlev1";
		try {
			boolean getLock = sempAll.tryAcquire(10, TimeUnit.HOURS);
			if (!getLock) {
				log.warn("middle No Locked");
				return;
			}
			log.info("middle Get Locked");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		int[] days = { 5 };
		double[] rates = { 3.5 };
		int op = 0;
		int mp = 0;
		try {
			for (int day : days) {
				for (double rate : rates) {
					List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
					log.info("codelist:" + codelist.size());
					String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
					String detailOther = ",rate=" + rate + ",day=" + day;
					log.info("startDate={},endDate={},{}", startDate, endDate, detailOther);
					try {
						List<TraceSortv2Vo> samples = Collections.synchronizedList(new LinkedList<TraceSortv2Vo>());
						// start..
						CountDownLatch cnt = new CountDownLatch(codelist.size());
						for (StockBaseInfo ss : codelist) {
							String code = ss.getCode();
							if (!stockBasicService.online2YearChk(code, ed)) {// 最后日需要满足2年
								log.info("{} 上市不足2年", ss.getCode());
								cnt.countDown();
								continue;
							}
							TasksWorkerModel.add(code, new TasksWorkerModelRunnable() {
								@Override
								public void running() {
									try {
										int twoYearChkDate = DateUtil.getNext2Year(Integer.valueOf(ss.getList_date()));// 每个交易日都会check2年
										// log.info("code={},date={}", code, date);
										List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService
												.queryListByCodeWithLastNofq(code, sd, ed,
														EsQueryPageUtil.queryPage9999, SortOrder.ASC);// 返回的list是不可修改对象
										for (TradeHistInfoDaliyNofq td : dailyList0) {
											int date = td.getDate();
											if (date < twoYearChkDate) {
												continue;
											}
											double todayChangeRate = td.getTodayChangeRate();
											try {
												LinePrice linePrice = new LinePrice(code, date,
														daliyTradeHistroyService);
												if (todayChangeRate >= 9.5 && td.getHigh() == td.getClosed()// 涨停
														&& !linePrice.isLowClosePriceToday(todayChangeRate,
																td.getYesterdayPrice(), td.getClosed(), td.getHigh())) {// 是否上影线
													DaliyBasicInfo basic = daliyBasicHistroyService
															.getDaliyBasicInfoByDate(code, date);
													EsQueryPageReq queryPage = null;
													if (basic.getCirc_mv() > 5000000) {// >500亿
														queryPage = EsQueryPageUtil.queryPage10;
													} else if (basic.getCirc_mv() > 1000000) {// >100亿 / 大盘股:流通市值小于100亿
														queryPage = EsQueryPageUtil.queryPage8;
													} else {
														// 小盘股
														queryPage = EsQueryPageUtil.queryPage5;
													}
													List<TradeHistInfoDaliy> findHigh = daliyTradeHistroyService
															.queryListByCodeWithLastQfq(code, date, 0, queryPage,
																	SortOrder.ASC);// 返回的list是不可修改对象
													TradeHistInfoDaliy highDaliy = findHighDate(findHigh);
													if (highDaliy != null) {
														int hiDate = highDaliy.getDate();
														// 8个月新高，涨幅未超55%
														if (linePrice.priceCheckForMiddle(code, hiDate)) {
															LineAvgPrice lineAvg = new LineAvgPrice(avgService);
															if (lineAvg.isWhiteHorseV3ForMiddle(code, hiDate)) {// 10日线的白马
																TradeHistInfoDaliyNofq buyDate = findBuyDate(code,
																		hiDate, rate);
																if (buyDate != null) {
																	TraceSortv2Vo t1 = saveOkRec(code,
																			buyDate.getDate(), day);
																	if (t1 != null) {
																		samples.add(t1);
																	} else {
																		log.info(
																				"middle error : {},{},buyDate={},TraceSortv2Vo  is null",
																				code, date, buyDate.getDate());
																	}
																} else {
																	log.info("middle error : {},{},buyDate  is null",
																			code, date);
																}
															} else {
																log.info("middle error : {},{},10日线不是白马", code, date);
															}
														} else {
															log.info("middle error : {},{},8个月新高，涨幅超55%", code, date);
														}
													} else {
														log.info("middle error : {},{},highDaliy is null", code, date);
													}
												}
											} catch (Exception e) {
												e.printStackTrace();
												ErrorLogFileUitl.writeError(e, code, date + "", "");
											}
										}
									} catch (Exception e) {
										ErrorLogFileUitl.writeError(e, code, sd + "", ed + "");
										e.printStackTrace();
									} finally {
										cnt.countDown();
									}
								}
							});
						}
						try {
							cnt.await();
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						int total_all = samples.size();
						log.info(version + " 获取样本数:" + total_all);
						if (total_all > 0) {
							TraceSortv2StatVo stat = new TraceSortv2StatVo();
							String filepath = FILE_FOLDER + version + "_" + startDate + "_" + ed + "_" + batch;
							stat(filepath, stat, samples);
							sendMessge(version, batch, startDate, ed + "", 0, 0, stat, total_all, sysstart, detailOther,
									op, mp);
						} else {
							WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + " 获取样本数为0");
						}

					} catch (Exception e) {
						e.printStackTrace();
						WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + " 样本出错");
					}
				}
			}
			WxPushUtil.pushSystem1(version + "样本完成！" + sd + " " + ed);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(version + "样本出错！" + sd + " " + ed);
		} finally {
			sempAll.release();
		}
	}

	// == 高低开 ==
	private boolean checkOpenPrice(int op, TradeHistInfoDaliyNofq td) {
		if (op == 0) {
			return true;
		}
		if (op == 1) {
			return isHighOpenPrice(td);
		}
		return isLowOpenPrice(td);
	}

	private boolean isHighOpenPrice(TradeHistInfoDaliyNofq td) {
		if (td.getOpen() > td.getYesterdayPrice()) {
			if (CurrencyUitl.cutProfit(td.getYesterdayPrice(), td.getOpen()) >= 2.0) {
				return true;
			}
		}
		return false;
	}

	private boolean isLowOpenPrice(TradeHistInfoDaliyNofq td) {
		return td.getOpen() <= td.getYesterdayPrice();
	}
	// == 高低开 ==

	// 中线-涨停后的最高交易日（最高日不是当天->涨停就回撤了; 也不是最后1天->一直在拉升）
	private TradeHistInfoDaliy findHighDate(List<TradeHistInfoDaliy> list) {
		TradeHistInfoDaliy dmax = list.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		if (dmax.getDate() != list.get(0).getDate() && dmax.getDate() != list.get(list.size() - 1).getDate()) {
			return dmax;
		}
		return null;
	}

	// 中线-最高交易日后的企稳和第一个3.5%以上
	private TradeHistInfoDaliyNofq findBuyDate(String code, int date, double rate) {
		List<TradeHistInfoDaliyNofq> listNofq = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, date, 0,
				EsQueryPageUtil.queryPage30, SortOrder.ASC);// 往后的30个交易日
		TradeHistInfoDaliyNofq highDaliy = listNofq.get(0);
		TradeHistInfoDaliyNofq beforeDate = null;
		for (TradeHistInfoDaliyNofq d : listNofq) {
			if (d.getTodayChangeRate() >= rate && d.getClosed() < highDaliy.getHigh()) {// 涨幅3.5%,切收盘价格小于最高价（说明有回撤）。
				if (beforeDate != null && beforeDate.getTodayChangeRate() < d.getTodayChangeRate()) {// 今日涨幅大于昨天涨幅
					LineAvgPrice lineAvg = new LineAvgPrice(avgService);
					if (lineAvg.isStable(code, beforeDate.getDate())) {// 企稳
						return d;
					}
				}
			}
			beforeDate = d;
		}
		return null;
	}
}
