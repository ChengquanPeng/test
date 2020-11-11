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

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsHistTraceDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.StrongService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.LogFileUitl;
import com.stable.utils.TasksWorkerModel;
import com.stable.utils.TasksWorkerModelRunnable;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.HistTrace;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.retrace.TraceSortv2StatVo;
import com.stable.vo.retrace.TraceSortv2Vo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SortV4Service {
	@Autowired
	private StockBasicService stockBasicService;
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

	private TraceSortv2Vo saveOkRec(String code, int date, int day) {
		try {
			TraceSortv2Vo t1 = new TraceSortv2Vo();
			t1.setCode(code);
			t1.setDate(date);
			t1.setMarketIndex(strongService.getIndexPrice(code, date));

			// date=已收盘的日期
			EsQueryPageReq queryPage6 = new EsQueryPageReq(day + 1);// 为啥+1:queryListByCodeWithLastQfq 包含当天了。
			List<TradeHistInfoDaliy> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, date, 0,
					queryPage6, SortOrder.ASC);// 返回的list是不可修改对象

			// 除去当天的交易日来算
			List<TradeHistInfoDaliy> dailyList2 = new ArrayList<TradeHistInfoDaliy>();
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
//			FinanceBaseInfo fin = financeService.getLastFinaceReport(code, date);
//			t1.setFin(fin);
//			// 最新快报&预告
//			int currYear = DateUtil.getYear(date);
//			int currJidu = DateUtil.getJidu(date);
//			// 当前季度的快预报（当前季度中后期）
//			if (fin != null && date > 20200101 && !getLastKygb(t1, code, date, currYear, currJidu)) {
//				int preYear = currYear;
//				int preJidu = currJidu - 1;
//				if (preJidu == 0) {
//					preYear = preYear - 1;
//					preJidu = 4;
//				}
//				// 上季度的快预报（当前季度初期）
//				if (!getLastKygb(t1, code, date, preYear, preJidu)) {
//
//					// 财务季度的快预报
//					if (fin != null) {
//						int finYear = DateUtil.getYear(fin.getDate());
//						int finJidu = DateUtil.getJidu(fin.getDate());
//						getLastKygb(t1, code, date, finYear, finJidu);
//					}
//				}
//			}
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

	private String title = "代码,日期,当日涨幅,当日大盘涨幅,一定亏损,是否盈利,买入价,卖出价,盈利多少,最高收盘价,最低收盘价,最低价幅度,最低价,最低价幅度,市盈率(静),市盈率(TTM),"
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
			LogFileUitl.writeFileWithGBK(filepath + ".csv", sb.toString());
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

	public void sortv4(String startDate, String endDate) {
		int batch = Integer.valueOf(DateUtil.getTodayYYYYMMDD_NOspit());
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
		String version = "sortv4";
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
		int[] days = { 3 };
		double maxRate = 6.5;
		double[] rates = { 3.5 };
		int op = 0;
		int mp = 0;
		try {
			for (int day : days) {
				for (double rate : rates) {
					List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
					log.info("codelist:" + codelist.size());
					String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
					String detailOther = ",rate=" + rate + ",day=" + day + ",";
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
										int lastBuyDate = 0;
										for (TradeHistInfoDaliyNofq td : dailyList0) {
											int date = td.getDate();
											try {
												if (date < twoYearChkDate) {
													continue;
												}
												TraceSortv2Vo t1 = middleRunningsortv4(code, date, td, rate, maxRate,
														day);
												if (t1 != null && lastBuyDate != t1.getDate()) {
													t1.setZhangtingDate(date);
													samples.add(t1);
													lastBuyDate = t1.getDate();// 不能存在同样的样本
												}
											} catch (Exception e) {
												ErrorLogFileUitl.writeError(e, code, sd + "", ed + " " + date);
												e.printStackTrace();
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
							if (!cnt.await(12, TimeUnit.HOURS)) {// 等待执行完成
								log.info("模型执行完成超时异常==>" + version);
							}
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						int total_all = samples.size();
						log.info(version + " 获取样本数:" + total_all);
						if (total_all > 0) {
							TraceSortv2StatVo stat = new TraceSortv2StatVo();
							String filepath = FILE_FOLDER + version + "_" + batch + "_" + startDate + "_" + ed;
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
			//WxPushUtil.pushSystem1(version + "样本完成！" + sd + " " + ed);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(version + "样本出错！" + sd + " " + ed);
		} finally {
			sempAll.release();
		}
	}

	public static void main(String[] args) {
	}

	private TraceSortv2Vo middleRunningsortv4(String code, int date, TradeHistInfoDaliyNofq td, double minRate,
			double maxRate, int day) {
		double todayChangeRate = td.getTodayChangeRate();
		try {
			if (isTodayPriceOk(minRate, maxRate, todayChangeRate, td.getYesterdayPrice(), td.getClosed(),
					td.getHigh())) {// 是否严重上影线
				// 1.前5个交易日,收盘价未超过10%
				if (isTradeOkBefor5(code, date, true)) {
					// 2.均线支持
					if (isWhiteHorseForSortV4(code, date, true)) {// 10日线的白马TODO 均线用不复权试试
						TraceSortv2Vo t1 = saveOkRec(code, date, day);
						if (t1 != null) {
							return t1;
						} else {
							log.info("middle error : {},{},buyDate={},TraceSortv2Vo  is null", code, date, date);
						}
					} else {
						log.info("middle error : {},{},5日线不是白马", code, date);
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, code, date + "", "");
		}
		return null;
	}

	/**
	 * 1.tody price check
	 */
	public boolean isTodayPriceOk(double minRate, double maxRate, double todayChangeRate, double yesterdayPrice,
			double closedPrice, double hightPrice) {
		if (todayChangeRate >= minRate && todayChangeRate <= maxRate) {
			LinePrice linePrice = new LinePrice();
			return !linePrice.isLowClosePriceToday(todayChangeRate, yesterdayPrice, closedPrice, hightPrice, 0.5);
		}
		return false;
	}

	/**
	 * 2.均线
	 */
	public boolean isWhiteHorseForSortV4(String code, int date, boolean isTrace) {
		LineAvgPrice lineAvg = new LineAvgPrice(avgService);
		return lineAvg.isWhiteHorseForSortV4(code, date, isTrace);
	}

	/**
	 * 3.前面交易
	 */
	public boolean isTradeOkBefor5(String code, int date, boolean isTrace) {
		EsQueryPageReq req = EsQueryPageUtil.queryPage6;
		if (!isTrace) {
			req = EsQueryPageUtil.queryPage5;
		}
		List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				req, SortOrder.DESC);// 返回的list是不可修改对象
		List<TradeHistInfoDaliyNofq> nlist = new ArrayList<TradeHistInfoDaliyNofq>();
		for (TradeHistInfoDaliyNofq td : dailyList0) {
			nlist.add(td);
		}

		if (isTrace) {// 实时不需要调用这个
			TradeHistInfoDaliyNofq today = nlist.get(0);
			nlist.remove(0);// 移除当天

			double total = 0.0;
			for (TradeHistInfoDaliyNofq d : nlist) {
				total += d.getVolume();
			}
			// 均值*基数
			double chkVol = Double.valueOf(total / nlist.size() * 1.8);
			if (today.getVolume() > chkVol) {
				log.info("当日明显放量近1倍 {},{}", code, date);
				// 明显放量的不能买
				return false;
			}
		}
		double highPice = nlist.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getClosed)).get()
				.getClosed();
		double lowPice = nlist.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getClosed)).get()
				.getClosed();
		if (CurrencyUitl.topPrice(lowPice, false) >= highPice) {
			return true;
			// 前一天收盘价比前面几天的最高价还高--过滤
//			TradeHistInfoDaliyNofq last = nlist.get(0);
//			nlist.remove(0);
//			boolean isPriceOk = false;
//			for (TradeHistInfoDaliyNofq td : nlist) {
//				if (td.getHigh() > last.getClosed()) {
//					isPriceOk = true;
//					break;
//				}
//			}
//			if (!isPriceOk) {
//				log.info("前一天的收盘价比前面几天的最高价还高 {},{}", code, date);
//			}
//			return isPriceOk;
		}
		log.info("前五交易日振幅过大{},{},highPice={},lowPice={}", code, date, highPice, lowPice);
		return false;
	}

	@PostConstruct
	public void test1() {
		new Thread(new Runnable() {
			@Override
			public void run() {
//				String code = "600789";
//				int date = 20191231;
//				String code = "603477";
//				int date = 20201030;
//				String code = "300750";
//				int date = 20200929;

//				String code = "600109";
//				int date = 20201105;
//
//				double rate = 3.5;
//				int day = 5;
//				List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code,
//						date, date, EsQueryPageUtil.queryPage9999, SortOrder.ASC);
//				if (dailyList0 != null && dailyList0.size() > 0) {
//					TradeHistInfoDaliyNofq td = dailyList0.get(0);
//					TraceSortv2Vo tv = middleRunningsortv4(code, date, td, rate, 7.5, day);
//					log.info(tv);
//				} else {
//					log.info("dailyList0 is null");
//				}
//				sortv4("20200101", "20201031");
//				sortv4("20190101", "20191231");
//				sortv4("20180101", "20181231");
//				sortv4("20170101", "20171231");
//				sortv4("20160101", "20161231");
//				sortv4("20110101", "20111231");
//				sortv4("20120101", "20121231");
//				sortv4("20130101", "20131231");
			}
		}).start();
	}
}
