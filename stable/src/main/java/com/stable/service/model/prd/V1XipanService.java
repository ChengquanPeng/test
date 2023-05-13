package com.stable.service.model.prd;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.StringUtil;
import com.stable.utils.TagUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class V1XipanService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

//	@javax.annotation.PostConstruct
	public void test() {
		String[] codes = { "600130", "002445", "002658", "002432", "000957" };
		int[] dates = { 20220831, 20220816, 20220822, 20211115, 20220516 };
		for (int i = 0; i < codes.length; i++) {
			String code = codes[i];
			int date = dates[i];

			CodeBaseModel2 newOne = new CodeBaseModel2();
			newOne.setZfjjup(2);
			newOne.setZfjjupStable(1);
			newOne.setCode(code);
			newOne.setPls(1);
			newOne.setHolderNumP5(50);
			System.err.println("==========" + stockBasicService.getCodeName2(code) + "==========");
			xipanQb(date, newOne, true);
			System.err.println("Res ==========> " + (newOne.getXipan() > 0) + ",CNT:" + newOne.getXipan() + ","
					+ newOne.getXipanHist());
		}
		System.exit(0);
	}

	List<TradeHistInfoDaliy> volDate = new LinkedList<TradeHistInfoDaliy>();
	private int preDays = 4;

	/** 起爆-Pre突破 */
	public void xipanQb(int date, CodeBaseModel2 newOne, boolean isSamll) {
		if (!(TagUtil.stockRange(isSamll, newOne) && newOne.getHolderNumP5() > 0 && newOne.getHolderNumP5() > 21.0)) {
			this.resetXiPan(newOne);
			return;
		}
		String code = newOne.getCode();
		List<TradeHistInfoDaliy> tl = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage80, SortOrder.DESC);
		List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy t : tl) {
			list.add(t);
		}

		List<TradeHistInfoDaliy> list5 = list.subList(3, 60).stream().filter(s -> s.getTodayChangeRate() > 0.0)
				.collect(Collectors.toList());// 去掉最新的2天
		List<TradeHistInfoDaliy> lt1 = list5.stream().filter(s -> s.getTodayChangeRate() >= 5.0)
				.collect(Collectors.toList());
		List<TradeHistInfoDaliy> lt2 = list5.stream().filter(s -> s.getTodayChangeRate() >= 9.8)
				.collect(Collectors.toList());

		boolean isqb = false;
		boolean chk1 = false;
		// 5.0%以上不能超过5天，涨停10%不能超过3天
		if (lt1.size() < 5 || lt2.size() < 3) {
			chk1 = true;
		}

		if (chk1) {
			int sz = list.size();
			volDate.clear();
			/** 1.15%新高 */

			boolean yd = true;
			double tot = 0.0;
			for (int j = 2; j < list.size(); j++) {
				TradeHistInfoDaliy chkday = list.get(j);
				if (chkday.getTodayChangeRate() >= 2.4) {// 1.上涨
					// System.err.println(code + "========>" + chkday.getDate());
					yd = true;
					tot = 0.0;

					/** 放量的后2天 */
					for (int i = j - 1; i >= j - 2; i--) {
						TradeHistInfoDaliy t = list.get(i);
						if (t.getTodayChangeRate() > 0.0) {// 下跌
							// System.err.println(" ChangeRate > 0.0 =" + t.getDate());
							if ((t.getOpen() > t.getClosed() && t.getTodayChangeRate() < 1)) {
								// 上涨不超过1%，且K线阴：收盘小于开盘
							} else {
								yd = false;
								break;
							}
						}
					}

					/** 放量的前的3天 */
					if (yd) {
						int days = 0;// 保证有3天
						for (int i = j + 1; i <= j + preDays; i++) {
							if (i < sz) {
								TradeHistInfoDaliy t = list.get(i);
								tot += t.getVolume();
								days++;
							}
						}
						if (days >= 3 && bigVolChk(chkday, tot, days)) {// 几乎倍量
							// System.err.println(chkday.getVolume() + "|" + ((tot / 3) * 1.8));
							volDate.add(chkday);
						}
					}
				}
			}
			int xipan = volDate.size();
			/** 如果最高的那天是第三天之前，且价格合适，则OK */
			/** 以第三天作为分界线，找到最高的那天。 */
			if (xipan > 0) {

				List<TradeHistInfoDaliy> list2 = list;
				TradeHistInfoDaliy last = list2.get(0);// 第一天
				TradeHistInfoDaliy d3t = list2.get(2);// 第三天
				TradeHistInfoDaliy high = list2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
						.get();

				if (d3t.getDate() > high.getDate() && high.getHigh() > last.getClosed()
						&& CurrencyUitl.cutProfit(last.getClosed(), high.getHigh()) <= 15) {// 15%以内冲新高

					// 1.1.找到第一个成交异动日
					int firstVolDate = volDate.stream().sorted(Comparator.comparing(TradeHistInfoDaliy::getDate))
							.collect(Collectors.toList()).get(0).getDate();// 突破中第一个日期
					// 1.2.从第一个反昨日开始后的最低价格那天(反转日)：
					int lowDate = list2.stream().filter(s -> s.getDate() >= firstVolDate)
							.min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get().getDate();

					// 1.3.找第一次放量日和最低价格日之间的所有上涨放量，且倒序排序[去掉下跌的量]，区间【第一个放量日和最低价格日之间】
					List<TradeHistInfoDaliy> list3 = list2.stream()
							.filter(s -> s.getDate() < lowDate && s.getTodayChangeRate() > 0.0)
							.sorted(Comparator.comparing(TradeHistInfoDaliy::getVolume).reversed())
							.collect(Collectors.toList());// 所有上涨date中最大的量

					// 1.4.异动日期中最大的量
					int maxVolDate = volDate.stream()
							.sorted(Comparator.comparing(TradeHistInfoDaliy::getVolume).reversed())
							.collect(Collectors.toList()).get(0).getDate();// 突破中最大的量

					boolean isNotOK = true;
					// 1.5.判断：如果放量是接近最大的那个，第一或者第二放量，则OK，否则放弃。
					if (list3.size() > 0) {
						// 最大的不是第一或者第二，没戏？
						isNotOK = list3.get(0).getDate() != maxVolDate;
						if (isNotOK && list3.size() > 1) {
							isNotOK = list3.get(1).getDate() != maxVolDate;
						}
					}
					// if (list3.get(0).getDate() != maxVolDate && list3.get(1).getDate() !=
					// maxVolDate) {
					if (!isNotOK) {
						// 是否挖坑，
						// 2.1找到放量日中，最低价格收盘价
						double chkPrice = volDate.stream()
								.sorted(Comparator.comparing(TradeHistInfoDaliy::getYesterdayPrice))
								.collect(Collectors.toList()).get(0).getYesterdayPrice();// 突破中最小的昨日收盘价格
						int cnt = list2.stream().filter(s -> s.getDate() > firstVolDate && s.getLow() < chkPrice)
								.collect(Collectors.toList()).size();
						// System.err.println("count=" + cnt);
						if (cnt >= 4) {
							newOne.setPrice3m(high.getHigh());
							isqb = true;
						}
					}
				}

				// 连续4天下跌或者影线
				if (isqb) {
					int inc = 0;
					boolean incOk = false;
					for (int i = list2.size() - 1; i >= 0; i--) {
						TradeHistInfoDaliy t = list2.get(i);
						if (t.getTodayChangeRate() < 0) {
							inc++;
						} else if (t.getOpen() >= t.getClosed() && t.getTodayChangeRate() < 1) {
							inc++;
						} else {
							inc = 0;
						}
						if (inc >= 4) {
							incOk = true;
//							System.err.println(t.getDate());
						}
					}
					isqb = incOk;
//					System.err.println(incOk);
				}

			}
		}

		if (isqb) {
			newOne.setXipan(volDate.size());
			newOne.setXipanHist(
					volDate.stream().map(s -> String.valueOf(s.getDate())).collect(Collectors.joining(",")));
		} else {
			resetXiPan(newOne);
		}
	}

	private boolean bigVolChk(TradeHistInfoDaliy chkday, double tot, int days) {
		double def = 1.8;
		double ch = 0.0;

		// 直接获取换手率
		if (chkday.getChangeHands() > 0) {
			ch = chkday.getChangeHands();
		} else {// 成交获取换手率
			try {
				DaliyBasicInfo2 db = daliyBasicHistroyService.queryByCodeAndDate(chkday.getCode(), chkday.getDate());
				double cjl = chkday.getVolume() * 100.0 / 10000.0;// 交易(万股)=手x100/10000;
				double totalshare = db.getTotalShare();
				if (totalshare <= 0) {
					totalshare = stockBasicService.getCode(chkday.getCode()).getTotalShare();
				}
				double wg = totalshare * 10000;
				ch = CurrencyUitl.roundHalfUp(cjl / wg);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, chkday.getCode(), chkday.getDate(), "");
			}
		}

		/** 根据换手率来计算倍量，常规换手低则倍量高，常规倍量适中则默认1.8 */
		if (ch > 0.0) {// 检查换手率
			if (ch < 1.5) {
				def = 2.2;
			} else if (ch <= 2.0) {// 放量的换手率在2%以下
				def = 2.0;
			}
		}

		return (chkday.getVolume() > ((tot / days) * def));
//		return (chkday.getVolume() > ((tot / preDays) * 1.8));
	}

	public void resetXiPan(CodeBaseModel2 newOne) {
		if (newOne.getXipan() > 0) {
			String jsHist = newOne.getXipanHist() + "洗盘" + ";" + newOne.getJsHist();
			newOne.setJsHist(StringUtil.subString(jsHist, 100));
		}
		newOne.setXipan(0);
		newOne.setXipanHist("");

		// V1XipanService 可能存在了设置，所以先判断
		if (newOne.getNxipan() <= 0) {
			newOne.setPrice3m(0);
		}
	}
}
