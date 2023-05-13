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
import com.stable.utils.TagUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class NxService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;

	// @javax.annotation.PostConstruct
	public void test() {
		String[] codes = { "600072", "002593" };
		int[] dates = { 20230424, 20220726 };

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
			nxipan(date, newOne, 100);
			System.err.println(code + " ==========> " + (newOne.getNxipan() > 0));
		}
		System.exit(0);
	}

	List<Integer> datesXi = new LinkedList<Integer>();
	List<Integer> datesLa = new LinkedList<Integer>();
	LinkedList<String> incstr = new LinkedList<String>();

	/** 起爆-Pre突破 */
	public void nxipan(int date, CodeBaseModel2 newOne, double mkv) {
		if (!TagUtil.stockRangeNx(newOne, mkv)) {
			this.resetNxiPan(newOne);
			return;
		}
		String code = newOne.getCode();
		List<TradeHistInfoDaliy> tl = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage60, SortOrder.DESC);
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
			int inc = 0;
			int datet = 0;
			datesXi.clear();
			datesLa.clear();
			incstr.clear();
			for (int i = list.size() - 1; i >= 0; i--) {
				TradeHistInfoDaliy t = list.get(i);
				if (t.getTodayChangeRate() < 0) {
					inc++;
				} else if (t.getOpen() >= t.getClosed() && t.getTodayChangeRate() < 1) {
					inc++;
				} else {

					if (inc >= 4) {
						datesXi.add(datet);
						incstr.add(datet + ":" + inc);
						// System.err.println(incstr.getLast());
					}
					inc = 0;
				}
				if (inc == 1) {
					datet = t.getDate();
				}
			}
			List<TradeHistInfoDaliy> rates = new LinkedList<TradeHistInfoDaliy>();
			for (int i = list.size() - 1; i >= 0; i--) {
				rates.add(list.get(i));
				if (rates.size() > 3) {
					rates.remove(0);

				}
				if (rates.size() == 3) {
					if (rates.stream().mapToDouble(TradeHistInfoDaliy::getTodayChangeRate).sum() > 15) {
						int la = rates.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get()
								.getDate();
						// System.err.println(la);
						datesLa.add(la);
					}
				}

				if (datesLa.size() > 0) {
					for (int lad : datesLa) {
						for (int xid : datesXi) {
							if (lad <= xid) {
								isqb = true;
							}
						}
					}
				}
			}
		}
		if (isqb) {
			newOne.setNxipan(1);
			String s1 = datesLa.stream().map(s -> String.valueOf(s)).collect(Collectors.joining(",")) + "|"
					+ incstr.stream().map(s -> s).collect(Collectors.joining(","));
			newOne.setNxipanHist(s1);
			//System.err.println(s1);
		} else {
			resetNxiPan(newOne);
		}
	}

	public void resetNxiPan(CodeBaseModel2 newOne) {
		newOne.setNxipan(0);
		newOne.setNxipanHist("");
		// newOne.setPrice3m(0);
	}
}
