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
import com.stable.utils.TagUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class QbXipanService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

//	@javax.annotation.PostConstruct
	public void test() {
		String[] codes = { "002445", "002658", "002432", "000957", "603021" };
		int[] dates = { 20220816, 20220822, 20211112, 20220516, 20220901 };
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
			System.err.println("Res ==========> " + (newOne.getQbXipan() > 0) + ",CNT:" + newOne.getXipan() + ","
					+ newOne.getXipanHist());
		}
		System.exit(0);
	}

	List<TradeHistInfoDaliy> volDate = new LinkedList<TradeHistInfoDaliy>();
	private int preDays = 3;

	/** 起爆-Pre突破 */
	public void xipanQb(int date, CodeBaseModel2 newOne, boolean isSamll) {
		if (!(TagUtil.stockRange(isSamll, newOne) && newOne.getHolderNumP5() > 0 && newOne.getHolderNumP5() > 21.0)) {
			this.resetXiPan(newOne);
			return;
		}
		String code = newOne.getCode();
		List<TradeHistInfoDaliy> tl = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage70, SortOrder.DESC);
		List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy t : tl) {
			list.add(t);
		}

		List<TradeHistInfoDaliy> list5 = list.subList(3, 60).stream().filter(s -> s.getTodayChangeRate() > 0.0)
				.collect(Collectors.toList());
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
						int ts = 0;// 保证有3天
						for (int i = j + 1; i <= j + preDays; i++) {
							if (i < sz) {
								TradeHistInfoDaliy t = list.get(i);
								tot += t.getVolume();
								ts++;
							}
						}
						if (ts == preDays && bigVolChk(chkday, tot)) {// 几乎倍量
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
				List<TradeHistInfoDaliy> list2 = list.subList(0, 60);
				TradeHistInfoDaliy last = list2.get(0);// 第一天
				TradeHistInfoDaliy d3t = list2.get(2);// 第三天
				TradeHistInfoDaliy high = list2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
						.get();

				if (d3t.getDate() > high.getDate() && high.getHigh() > last.getClosed()
						&& CurrencyUitl.cutProfit(last.getClosed(), high.getHigh()) <= 15) {// 15%以内冲新高
					// 放量是接近最大的那个[3-60，去掉下跌的量]
					List<TradeHistInfoDaliy> list3 = list5.stream()
							.sorted(Comparator.comparing(TradeHistInfoDaliy::getVolume).reversed())
							.collect(Collectors.toList());// 所有上涨date中最大的量
					List<TradeHistInfoDaliy> list4 = volDate.stream()
							.sorted(Comparator.comparing(TradeHistInfoDaliy::getVolume).reversed())
							.collect(Collectors.toList());// 突破中最大的

					if (list3.get(0).getDate() != list4.get(0).getDate()
							&& list3.get(1).getDate() != list4.get(0).getDate()) {
						// 最大的不是第一或者第二，没戏？
						// System.err.println("Max:" + list4.get(0).getDate() + " -> " +
						// list3.get(0).getDate() + ","
						// + list3.get(1).getDate());
					} else {
						newOne.setPrice3m(high.getHigh());
						isqb = true;
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

				newOne.setXipan(xipan);
				newOne.setXipanHist(
						volDate.stream().map(s -> String.valueOf(s.getDate())).collect(Collectors.joining(",")));
			}
		}

		if (isqb) {
			newOne.setQbXipan(1);
		} else {
			newOne.setQbXipan(0);
			newOne.setPrice3m(0);
		}
	}

	private boolean bigVolChk(TradeHistInfoDaliy chkday, double tot) {
		double def = 1.8;
		double ch = 0.0;

		// 直接获取换手率
		if (chkday.getChangeHands() > 0) {
			ch = chkday.getChangeHands();
		} else {// 成交获取换手率
			try {
				DaliyBasicInfo2 db = daliyBasicHistroyService.queryByCodeAndDate(chkday.getCode(), chkday.getDate());
				double cjl = chkday.getVolume() * 100.0 / 10000.0;// 交易(万股)=手x100/10000;
				double wg = db.getTotalShare() * 10000;
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

		return (chkday.getVolume() > ((tot / preDays) * def));
//		return (chkday.getVolume() > ((tot / preDays) * 1.8));
	}

	public void resetXiPan(CodeBaseModel2 newOne) {
		newOne.setXipan(0);
		newOne.setXipanHist("");
		newOne.setQbXipan(0);
		newOne.setPrice3m(0);
	}
}
