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
import com.stable.service.model.WebModelService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.TagUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.http.req.ModelReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class V2NXipanService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;

	@Autowired
	private WebModelService webModelService;

//	@javax.annotation.PostConstruct
	public void test() {
		String[] codes1 = { "600072", "002593", "300238" };
		int[] dates1 = { 20230424, 20220726, 20230510 };
//		String[] codes1 = { "600072", "002593", "002445", "603608" };
//		int[] dates1 = { 20230424, 20220726, 20240222, 20240222 };
//		String[] codes1 = { "300238" };
//		int[] dates1 = { 20230510 };
		fortestingOnly(codes1, dates1);
		for (int i = 0; i < codes.length; i++) {
			String code = codes[i];
			int date = dates[i];

			CodeBaseModel2 newOne = new CodeBaseModel2();
			newOne.setZfjjup(2);
			newOne.setZfjjupStable(1);
			newOne.setCode(code);
			newOne.setPls(1);
			newOne.setHolderNumP5(50);
			newOne.setActMkv(100);
//			System.err.println("==========" +  + "==========");
			nxipan(date, newOne, 0);
			System.err.println(stockBasicService.getCodeName2(code) + " ==========> " + (newOne.getNxipan() > 0));
		}
		System.exit(0);
	}

	String[] codes = { "600072", "002593", "300238" };
	int[] dates = { 20230424, 20220726, 20230510 };

	private void fortestingOnly(String[] c, int[] d) {
		ModelReq mr = new ModelReq();
		mr.setNxipan(1);
		List<CodeBaseModel2> list = new LinkedList<CodeBaseModel2>();
		list = webModelService.getList(mr, EsQueryPageUtil.queryPage9999);
		System.err.println("list:size:" + list.size());
		if (list != null && list.size() > 0) {
			String[] t1 = new String[c.length + list.size()];
			int[] t2 = new int[t1.length];
			for (int i = 0; i < c.length; i++) {
				t1[i] = c[i];
			}
			for (int i = 0; i < d.length; i++) {
				t2[i] = d[i];
			}

			for (int i = c.length; i < list.size() + c.length; i++) {
				t1[i] = list.get(i - c.length).getCode();
				t2[i] = 20240308;
			}

			codes = t1;
			dates = t2;
		}
	}

	LinkedList<Integer> datesXi = new LinkedList<Integer>();
	LinkedList<Integer> datesLa = new LinkedList<Integer>();
	LinkedList<Double> datesLaPrice = new LinkedList<Double>();
	LinkedList<String> incstr = new LinkedList<String>();

	/** 起爆-Pre突破 */
	public void nxipan(int date, CodeBaseModel2 newOne, int nextTadeDate) {
		if (!TagUtil.stockRangeNx(newOne)) {
			log.info("不在stockRange Nx范围:" + newOne.getCode());
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

		boolean chk1 = false;
		boolean chk2 = false;
		boolean isqb = false;
		// 5.0%以上不能超过5天，涨停10%不能超过3天
		if (lt1.size() < 5 || lt2.size() < 3) {
			chk1 = true;
		}

		if (chk1) {
			int inc = 0;
			int datet = 0;
			datesXi.clear();
			datesLa.clear();
			datesLaPrice.clear();
			incstr.clear();
			// -找出连续下跌3、4天的洗盘票
			for (int i = list.size() - 1; i >= 0; i--) {
				TradeHistInfoDaliy t = list.get(i);
				if (t.getTodayChangeRate() < 0) {
					inc++;
				} else if (t.getOpen() >= t.getClosed() && t.getTodayChangeRate() < 1) {
					inc++;
				} else {

					// 连续洗盘3天
					if (inc >= 3) {
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

			// -找出连续3天上涨超过15%的拉伸票
			List<TradeHistInfoDaliy> rates = new LinkedList<TradeHistInfoDaliy>();
			for (int i = list.size() - 1; i >= 0; i--) {
				rates.add(list.get(i));
				if (rates.size() > 3) {
					rates.remove(0);

				}
				if (rates.size() == 3) {
					if (rates.stream().mapToDouble(TradeHistInfoDaliy::getTodayChangeRate).sum() > 15) {
						TradeHistInfoDaliy td = rates.stream()
								.max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
						int la = td.getDate();
						// System.err.println(la);
						if (!datesLa.contains(la)) {
							datesLa.add(la);
							datesLaPrice.add(td.getHigh());
						}
					}
				}
			}
			// -找出拉伸后有洗盘的情况
			// int chkLadate = 0;
			// int chkXidate = 0;

			if (datesLa.size() > 0) {
				for (int k = datesLa.size() - 1; k >= 0; k--) {// 最后拉升开始匹配
					int lad = datesLa.get(k);
					for (int xid : datesXi) {
						// System.err.println("lad:" + lad + " ,xid:" + xid);
						if (lad <= xid) {
							chk2 = true;
							// chkLadate = lad;
							// chkXidate = xid;
							break;
						}
					}
					if (chk2) {
						break;
					}
				}
			}
		} else {
			log.info("Nx chk1 is false:" + code);
		}

		if (chk2) {
			List<TradeHistInfoDaliy> list_asc = new LinkedList<TradeHistInfoDaliy>();
			// 翻转顺序，为正序。
			for (int i = list.size(); i > 0; i--) {
				list_asc.add(list.get(i - 1));
			}
			boolean t = isDanyanBuPo(list_asc);

			if (t) {
				isqb = predibuChk(code);
			}
		} else {
			log.info("Nx 拉升匹配洗盘失败:" + code);
		}

		if (isqb) {
			if (newOne.getTipNxing() == 0) {
				newOne.setTipNxing(nextTadeDate);
			}
			newOne.setNxipan(1);
			String s1 = datesLa.stream().map(s -> String.valueOf(s)).collect(Collectors.joining(",")) + "|"
					+ incstr.stream().map(s -> s).collect(Collectors.joining(","));
			newOne.setNxipanHist(s1);
			// System.err.println(s1);
			if (newOne.getPrice3m() <= 0) {
				newOne.setPrice3m(
						list5.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get().getHigh());
			}
		} else {
			resetNxiPan(newOne);
		}
	}

	private boolean predibuChk(String code) {
		// 排除3：半年内已经拉大幅下跌的。
		// 1.之前没有《连续20个交易日》振幅（涨/跌）超过45%的
		for (int k = datesLa.size() - 1; k >= 0; k--) {
			int la = datesLa.get(k);
			List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, la,
					EsQueryPageUtil.queryPage60, SortOrder.DESC);
			List<TradeHistInfoDaliy> list2 = list.subList(0, 20);

			// 排除3：半年内已经拉大幅下跌的。
			// 1.之前没有《连续25个交易日》跌幅超过50%的
			TradeHistInfoDaliy topDate = list2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
					.get();
			TradeHistInfoDaliy lowDate = list2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow))
					.get();

			if (CurrencyUitl.cutProfit(lowDate.getLow(), topDate.getHigh()) >= 48) {
				log.info("Nx 20个交易日振幅(涨/跌)超过48%");
				return false;
			}
			// 排除4：前面45个交易日（2个月）的最高价，没有现在高。
			List<TradeHistInfoDaliy> tmpl4 = list.subList(0, 45);
			TradeHistInfoDaliy topDate4 = tmpl4.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
					.get();
			// 最高价超过当前价
			if (topDate4.getHigh() > datesLaPrice.get(k)) {
				log.info("topDate1-10,date=" + topDate4.getDate() + ",high price:" + topDate4.getHigh() + ",chk price:"
						+ datesLaPrice.get(k));
				log.info("Nx 前面45个交易日（2个月）的最高价，没有现在高。（下跌反弹不算）");
				return false;
			}
		}
		return true;
	}

	// 是否丹阳不破
	private boolean isDanyanBuPo(List<TradeHistInfoDaliy> list) {
		for (int k = datesLa.size() - 1; k >= 0; k--) {// 最后拉升开始匹配
			int lad = datesLa.get(k);
			// System.err.println("chk:" + lad);
			for (int xid : datesXi) {
				// System.err.println("xid:" + xid);
				if (lad <= xid) {
					if (!getLa3DaysMinClosedPriceChk(lad, list)) {
						// 丹阳不破检查不合格。
						log.info("Nx 丹阳不破-破了");
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean getLa3DaysMinClosedPriceChk(int chkDate, List<TradeHistInfoDaliy> list) {
//		List<TradeHistInfoDaliy> t = LList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliy> befor = null;
		List<TradeHistInfoDaliy> aft = null;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getDate() == chkDate) {
				if (i <= 2) {
					befor = list.subList(0, i + 1);
				} else {
					befor = list.subList(i - 2, i + 1);
				}
				aft = list.subList(i + 1, list.size());
			}
		}
		if (befor != null) {
			double min = befor.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get().getLow();
			int inc = 0;
			for (TradeHistInfoDaliy t : aft) {
				if (min > t.getClosed()) {
					inc++;
				}
			}
			if (inc >= 3) {
				log.info("Nx 丹阳不破，破了" + inc + "次,chkDate=" + chkDate + ",minPrice=" + min);// 返回false
				return false;
			}
			return true;// 无丹阳不破
		}
		return false;
	}

	public static void main(String[] args) {

		List<Integer> list = new LinkedList<>();
		for (int i = 0; i < 20; i++) {
			list.add(i);
		}

		List<Integer> t1 = list.subList(1, 11);
		for (int i = 0; i < t1.size(); i++) {
			System.err.println(t1.get(i));
		}
		System.err.println("===========");
		int chkDate = 20;
		List<Integer> res = null;
		List<Integer> aft = null;
//			List<TradeHistInfoDaliy> t = LList<TradeHistInfoDaliy>();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == chkDate) {
				if (i <= 2) {
					res = list.subList(0, i + 1);
				} else {
					res = list.subList(i - 2, i + 1);
				}

				aft = list.subList(i + 1, list.size());
			}
		}

		if (res != null) {
			for (int i = 0; i < res.size(); i++) {
				System.err.println(res.get(i));
			}
			System.err.println("===========");
			for (int i = 0; i < aft.size(); i++) {
				System.err.println(aft.get(i));
			}
		} else {
			System.err.println("res is null");
		}
	}

	public void resetNxiPan(CodeBaseModel2 newOne) {
		newOne.setNxipan(0);
		newOne.setTipNxing(0);
		newOne.setNxipanHist("");

		// V1XipanService 可能存在了设置，所以先判断
		if (newOne.getXipan() <= 0) {
			newOne.setPrice3m(0);
		}
	}
}
