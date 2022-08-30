package com.stable.service.model.prd;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.TagUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class QbXipanService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;

//	@javax.annotation.PostConstruct
	public void test() {
		String[] codes = { "002445", "002658", "002432", "000957", "603366" };
		int[] dates = { 20220816, 20220822, 20211112, 20220516, 20220726 };
		for (int i = 0; i < codes.length; i++) {
			String code = codes[i];
			int date = dates[i];

			CodeBaseModel2 newOne = new CodeBaseModel2();
			newOne.setZfjjup(2);
			newOne.setZfjjupStable(1);
			newOne.setCode(code);
			newOne.setPls(1);
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
		if (!TagUtil.stockRange(isSamll, newOne)) {
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

		int sz = list.size();

		volDate.clear();
		/** 1.15%新高 */

		boolean yd = true;
		double tot = 0.0;
		for (int j = 2; j < list.size(); j++) {
			TradeHistInfoDaliy chkday = list.get(j);
			if (chkday.getTodayChangeRate() >= 2.0) {// 1.上涨
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
					if (ts == preDays && chkday.getVolume() > ((tot / preDays) * 1.8)) {// 几乎倍量
						// System.err.println(chkday.getVolume() + "|" + ((tot / 3) * 1.8));
						volDate.add(chkday);
					}
				}
			}
		}
		int xipan = volDate.size();

		boolean isqb = false;
		/** 如果最高的那天是第三天之前，且价格合适，则OK */
		/** 以第三天作为分界线，找到最高的那天。 */
		if (xipan > 0) {
			List<TradeHistInfoDaliy> list2 = list.subList(0, 60);
			TradeHistInfoDaliy last = list2.get(0);// 第一天
			TradeHistInfoDaliy d3t = list2.get(2);// 第三天
			TradeHistInfoDaliy high = list2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();

			if (d3t.getDate() > high.getDate() && high.getHigh() > last.getClosed()
					&& CurrencyUitl.cutProfit(last.getClosed(), high.getHigh()) <= 15) {// 15%以内冲新高

				// 放量是接近最大的那个[3-60，去掉下跌的量]
				List<TradeHistInfoDaliy> list3 = list.subList(3, 60).stream().filter(s -> s.getTodayChangeRate() > 0.0)
						.collect(Collectors.toList()).stream()
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
			newOne.setXipan(xipan);
			newOne.setXipanHist(
					volDate.stream().map(s -> String.valueOf(s.getDate())).collect(Collectors.joining(",")));
		}

		if (isqb) {
			newOne.setQbXipan(1);
		} else {
			newOne.setQbXipan(0);
			newOne.setPrice3m(0);
		}
	}

	public void resetXiPan(CodeBaseModel2 newOne) {
		newOne.setXipan(0);
		newOne.setXipanHist("");
		newOne.setQbXipan(0);
		newOne.setPrice3m(0);
	}
}
