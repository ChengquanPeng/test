package com.stable.service.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import com.stable.service.CodePoolService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.StrongService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.LogFileUitl;
import com.stable.utils.TasksWorkerModel;
import com.stable.utils.TasksWorkerModelRunnable;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodePool;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.HistTrace;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.retrace.TraceSortv2StatVo;
import com.stable.vo.retrace.TraceSortv2Vo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SortV6Service {
	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public void sortv6(int tradeDate) {
		StringBuffer msg = new StringBuffer();
		StringBuffer msg2 = new StringBuffer();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		Map<String, CodePool> map = codePoolService.getCodePoolMap();
		List<CodePool> list = new LinkedList<CodePool>();
		for (StockBaseInfo s : codelist) {
			String code = s.getCode();
			boolean onlineYear = stockBasicService.online1YearChk(code, tradeDate);
			if (!onlineYear) {
				log.info("{},Online 上市不足1年", code);
				continue;
			}
			CodePool cp = map.get(code);

			// 短线模型6
			if (isWhiteHorseForSortV6(is15DayTodayPriceOk(code, tradeDate))) {
				if (cp.getSortMode6() == 0) {
					msg.append(code).append(",");
					cp.setSortMode6(1);
				}

			} else {
				cp.setSortMode6(0);
			}
			// 短线模型7（箱体震荡新高，是否有波浪走势）
			if (isWhiteHorseForSortV7(code, tradeDate)) {
				if (cp.getSortMode7() == 0) {
					msg2.append(code).append(",");
					cp.setSortMode7(1);
				}
			} else {
				cp.setSortMode7(0);
			}
			list.add(cp);
		}
		codePoolService.saveAll(list);
		if (msg.length() > 0) {
			WxPushUtil.pushSystem1(
					"短线模型6(前期3-50%吸筹，深度回踩突然涨停后再2-5个交易日回踩拉起,涨停日不放量，超过涨停价格后买入，买入2内未大幅拉升放弃):" + msg.toString());
		}
		if (msg2.length() > 0) {
			WxPushUtil.pushSystem1("短线模型7(箱体震荡新高,是否有波浪走势):" + msg2.toString());
		}
	}

	/**
	 * 1.15个交易日内有9.5%的涨幅,且涨停日有回调
	 */
	private TradeHistInfoDaliyNofq is15DayTodayPriceOk(String code, int date) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage16, SortOrder.DESC);

		// 变倒序为顺序
		List<TradeHistInfoDaliyNofq> asc = new LinkedList<TradeHistInfoDaliyNofq>();
		for (TradeHistInfoDaliyNofq r : l2) {
			asc.add(0, r);
		}

		// 有大涨的交易日
		boolean hasTopPrice = false;
		TradeHistInfoDaliyNofq topDate = null;
		for (TradeHistInfoDaliyNofq r : asc) {
			if (r.getTodayChangeRate() >= 9.5) {
				topDate = r;
				hasTopPrice = true;
				break;
			}
		}
		boolean priceOK = false;
		if (hasTopPrice) {
			int c = 0;
			// 往后的5个交易日收盘低于涨停价（回调）
			for (TradeHistInfoDaliyNofq r : asc) {
				if (r.getDate() > topDate.getDate() && c <= 5) {
					c++;
//					if (c == 1) {// 涨停第二天：是否直接低开低走
//						if (topDate.getClosed() > r.getHigh()) {
//							log.info("{} 涨停日 :{}，第二日:{} 第二天低开低走", code, topDate.getDate(), r.getDate());
//							return null;
//						}
//					}
					if (r.getClosed() < topDate.getYesterdayPrice()) {
						log.info("{} {} 回调过多", code, date);
						return null;
					}
					if (r.getClosed() < topDate.getClosed()) {
						priceOK = true;
						break;
					}
				}
			}
		} else {
			log.info("{} {} 最近15个工作日无大涨交易", code, date);
		}

		if (priceOK) {
			// 涨停日前2个月44个交易日是否有吸货嫌疑
			List<TradeHistInfoDaliyNofq> l3 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0,
					topDate.getDate(), EsQueryPageUtil.queryPage45, SortOrder.DESC);

			TradeHistInfoDaliyNofq bfTopDate = null;
			double bfTopDatePrice = 0.0;
			for (TradeHistInfoDaliyNofq r : l3) {
				if (r.getDate() != topDate.getDate()) {
					if (r.getHigh() > bfTopDatePrice) {
						bfTopDatePrice = r.getHigh();
						bfTopDate = r;
					}
				}
			}

			// 涨停前的最高交易日差价是否有吸货行为，是否有涨停
			boolean hasBfTop = false;
			if (bfTopDatePrice > topDate.getHigh()) {
				double bfLowPrice = 999999999.99;
				double bfLowPrice2 = 0.0;
				for (TradeHistInfoDaliyNofq r : l3) {
					if (r.getDate() < bfTopDate.getDate()) {// 高点以前的拉升
						if (r.getLow() < bfLowPrice) {
							bfLowPrice = r.getLow();
						}
						if (r.getTodayChangeRate() >= 9.5) {
							hasBfTop = true;
						}
					} else if (r.getDate() > bfTopDate.getDate() && r.getDate() < topDate.getDate()) {// 高点以后的回调
						if (bfLowPrice2 == 0.0 || bfLowPrice2 > r.getLow()) {
							bfLowPrice2 = r.getLow();
						}
					}
				}
				// 有涨停且是否有超过30%的拉升
				double persent = CurrencyUitl.cutProfit(bfLowPrice, bfTopDatePrice);
				double persent2 = CurrencyUitl.cutProfit(bfLowPrice2, bfTopDatePrice);
				// log.info("{} {} {}", bfLowPrice, bfTopDatePrice, persent);
				if (hasBfTop && persent >= 25.0 && persent <= 50.0 && persent2 >= 18.0
						&& topDate.getClosed() < bfTopDatePrice) {
					// 吸货之前的高点，未超过35%的整幅
					List<TradeHistInfoDaliy> l4 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
							l3.get(l3.size() - 1).getDate(), EsQueryPageUtil.queryPage100, SortOrder.DESC);
					double maxPrice = l4.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get()
							.getHigh();
					double minPrice = l4.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get()
							.getLow();

					// log.info("{} {} ", minPrice, maxPrice);

					double persent3 = CurrencyUitl.cutProfit(minPrice, maxPrice);
					if (persent3 <= 50.0 && isTradeOkBefor5ForVol(code, topDate.getDate())) {
						return topDate;
					}
				}
			}
			log.info("{} {} 无明显吸货嫌疑", code, date);
		} else {
			log.info("{} {} 无回调", code, date);
		}
		return null;
	}

	/**
	 * 2.均线
	 */
	private boolean isWhiteHorseForSortV6(TradeHistInfoDaliyNofq topDate) {
		if (topDate != null) {
			String code = topDate.getCode();
			EsQueryPageReq req = EsQueryPageUtil.queryPage30;
			// 最近30条-倒序
			List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLast60(code, topDate.getDate(), req,
					true);
			StockAvgBase sa = clist30.get(0);
			if (sa.getAvgPriceIndex30() >= sa.getAvgPriceIndex60()) {
				return true;
			}
			log.info("{} 均线不满足", code);
		}
		return false;
	}

	/**
	 * 箱体新高
	 */
	private boolean isWhiteHorseForSortV7(String code, int date) {
		List<TradeHistInfoDaliy> l1 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage5, SortOrder.DESC);
		TradeHistInfoDaliy maxDate = l1.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed)).get();

		List<TradeHistInfoDaliy> l2 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
				l1.get(l1.size() - 1).getDate(), EsQueryPageUtil.queryPage120, SortOrder.DESC);// 用5个交易日的最后一个

		List<TradeHistInfoDaliy> l3 = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy r : l2) {
			l3.add(r);
		}
		l3.remove(0);// 移除当天
		TradeHistInfoDaliy maxDate2 = l3.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		// 是否半年收盘新高
		if (maxDate.getDate() != maxDate2.getDate() && maxDate.getClosed() > maxDate2.getHigh()) {
			double maxPrice = maxDate2.getHigh();
			TradeHistInfoDaliy td = l3.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
			double minPrice = td.getLow();

			double persent3 = CurrencyUitl.cutProfit(minPrice, maxPrice);
			log.info("{} {},min:{}->{} max:{}->{} 半年整幅{}%", code, date, td.getDate(), td.getLow(), maxDate2.getDate(),
					maxDate2.getHigh(), persent3);
			if (persent3 <= 40.0) {
				return true;
			}
		}
		log.info("{} {} 最近5个交易日收盘不是半年新高", code, date);
		return false;
	}

//	@PostConstruct
//	private void test() {
//		String code = "600798";
//		if (isWhiteHorseForSortV6(is15DayTodayPriceOk(code, 20201217))) {
//			log.info("{} 满足条件", code);
//		} else {
//			log.info("{} 不满足条件", code);
//		}
//	}

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private EsHistTraceDao esHistTraceDao;
	@Autowired
	private StrongService strongService;
	@Autowired
	private TradeCalService tradeCalService;

	private String FILE_FOLDER = "/my/free/pvhtml/";

	public static final Semaphore sempAll = new Semaphore(1);

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
			if (dailyList2.size() > 0) {

				// 最高盈利
				double maxPrice = dailyList2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed))
						.get().getClosed();
				// 最高亏损
				double minPrice = dailyList2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed))
						.get().getClosed();
				// 最低价格
				double minLowPrice = dailyList2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow))
						.get().getLow();
				t1.setMinLowPrice(minLowPrice);
				t1.setBuyPrice(d0.getClosed());
				t1.setMaxPrice(maxPrice);
				t1.setMinPrice(minPrice);
				t1.setSellPrice(dailyList0.get(dailyList0.size() - 1).getClosed());
				t1.setBuyDayRate(d0.getTodayChangeRate());
				DaliyBasicInfo basic = daliyBasicHistroyService.getDaliyBasicInfoByDate(code, date);
				t1.setBasic(basic);
			}
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
		ht.setAct_percent(
				CurrencyUitl.roundHalfUpWhithPercent(Double.valueOf(stat.getAct_cnt_up()) / Double.valueOf(total_all)));
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

	public void sortv6(String startDate, String endDate) {
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
		String version = "sortv6";
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
		int op = 0;
		int mp = 0;
		try {
			for (int day : days) {
				List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
				log.info("codelist:" + codelist.size());
				String sysstart = DateUtil.getTodayYYYYMMDDHHMMSS();
				String detailOther = "first";
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
//						if (!"300661".equals(code)) {
//							continue;
//						}
						TasksWorkerModel.add(code, new TasksWorkerModelRunnable() {
							@Override
							public void running() {
								try {
									List<TraceSortv2Vo> codesamples = Collections
											.synchronizedList(new LinkedList<TraceSortv2Vo>());
									int twoYearChkDate = DateUtil.getNext2Year(Integer.valueOf(ss.getList_date()));// 每个交易日都会check2年
									// log.info("code={},date={}", code, date);
									List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService
											.queryListByCodeWithLastNofq(code, sd, ed, EsQueryPageUtil.queryPage9999,
													SortOrder.ASC);// 返回的list是不可修改对象
									for (TradeHistInfoDaliyNofq td : dailyList0) {
										int date = td.getDate();
										try {
											if (date < twoYearChkDate) {
												continue;
											}
											middleRunningsortv6(codesamples, code, date, day);
										} catch (Exception e) {
											ErrorLogFileUitl.writeError(e, code, sd + "", ed + " " + date);
											e.printStackTrace();
										}
									}
									if (codesamples.size() > 0) {
										samples.addAll(codesamples);
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
						if (startDate.equals(endDate)) {// 每日晚间
							StringBuffer sb = new StringBuffer();
							for (TraceSortv2Vo t1 : samples) {
								sb.append(t1.getCode()).append(" ");
							}
							WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + " 获取样本代码:" + sb.toString());
						} else {
							TraceSortv2StatVo stat = new TraceSortv2StatVo();
							String filepath = FILE_FOLDER + version + "_" + batch + "_" + startDate + "_" + ed + "_"
									+ day;
							stat(filepath, stat, samples);
							sendMessge(version, batch, startDate, ed + "", day, 0, stat, total_all, sysstart,
									detailOther, op, mp);
						}
					} else {
						WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + " 获取样本数为0");
					}

				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1(version + "样本区间:" + sd + " " + ed + " 样本出错");
				}
			}
			// WxPushUtil.pushSystem1(version + "样本完成！" + sd + " " + ed);
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(version + "样本出错！" + sd + " " + ed);
		} finally {
			sempAll.release();
		}
	}

	EsQueryPageReq req00 = EsQueryPageUtil.queryPage6;

	private TraceSortv2Vo middleRunningsortv6(List<TraceSortv2Vo> codesamples, String code, int date, int day) {
		try {
			TradeHistInfoDaliyNofq topDate = is15DayTodayPriceOk(code, date);
			if (isWhiteHorseForSortV6(topDate)) {

				List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code,
						topDate.getDate(), 0, req00, SortOrder.ASC);
				for (TradeHistInfoDaliyNofq r : dailyList0) {
					if (r.getDate() >= date && r.getDate() > topDate.getDate() && r.getTodayChangeRate() > 3.0
							&& r.getClosed() > topDate.getClosed()) {
						TraceSortv2Vo t1 = saveOkRec(code, r.getDate(), day);// find buy Date
						if (t1 != null && !isExist(codesamples, r.getDate(), 30)) {
							t1.setZhangtingDate(date);
							codesamples.add(t1);
						}
						return t1;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, code, date + "", "");
		}
		return null;
	}

	private boolean isExist(List<TraceSortv2Vo> codesamples, int date, int day) {
		if (codesamples.size() > 0) {
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

	/**
	 * 5.前面交易-交易量
	 */
	public boolean isTradeOkBefor5ForVol(String code, int date) {
		EsQueryPageReq req = EsQueryPageUtil.queryPage6;
		List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				req, SortOrder.DESC);// 返回的list是不可修改对象
		List<TradeHistInfoDaliyNofq> nlist = new ArrayList<TradeHistInfoDaliyNofq>();
		for (TradeHistInfoDaliyNofq td : dailyList0) {
			nlist.add(td);
		}

		// 实时不需要调用这个
		TradeHistInfoDaliyNofq today = nlist.get(0);
		nlist.remove(0);// 移除当天
		double vol = today.getVolume();
		double total = 0.0;
		int endIndx = nlist.size();// 涨幅较大，计算5天
		for (int i = 0; i < endIndx; i++) {
			total += nlist.get(i).getVolume();
		}
		// 均值*基数
		double chkVol = Double.valueOf(total / endIndx * 1.8);
		if (vol > chkVol) {
			log.info("当日明显放量近1倍 {},{}", code, date);
			// 明显放量的不能买
			return false;
		}
		return true;

	}

	@PostConstruct
	public void test6() {
		new Thread(new Runnable() {
			@Override
			public void run() {
//				String code = "600789";
//				int date = 20200117;
//				String code = "603477";
//				int date = 20201030;
//				String code = "300750";
//				int date = 20200929;

//				String code = "000625";
//				int date = 20201113;
//
//				double rate = 3.5;
//				int day = 3;
//				List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code,
//						date, date, EsQueryPageUtil.queryPage9999, SortOrder.ASC);
//				if (dailyList0 != null && dailyList0.size() > 0) {
//					TradeHistInfoDaliyNofq td = dailyList0.get(0);
//					TraceSortv2Vo tv = middleRunningsortv4(code, date, td, rate, 7.5, day);
//					log.info(tv);
//				} else {
//					log.info("dailyList0 is null");
//				}
//
//				String code = "600798";
//				int date = 20201218;
//				String code = "002765";
//				int date = 20201231;
//				if (isWhiteHorseForSortV6(is15DayTodayPriceOk(code, date))) {
//					log.info("{} 满足条件", code);
//				} else {
//					log.info("{} 不满足条件", code);
//				}

//				sortv6(20201231);
//				sortv6("20200101", "20201225");
//				sortv6("20190101", "20191231");
//				sortv6("20180101", "20181231");
//				sortv6("20170101", "20171231");
//				sortv6("20160101", "20161231");
//				sortv4("20110101", "20111231");
//				sortv4("20120101", "20121231");
//				sortv4("20130101", "20131231");

//				String code = "600789";
//				int date = 20191231;
//				if (isWhiteHorseForSortV7(code, date)) {
//					log.info("{} 满足条件", code);
//				} else {
//					log.info("{} 不满足条件", code);
//				}
			}
		}).start();
	}

}
