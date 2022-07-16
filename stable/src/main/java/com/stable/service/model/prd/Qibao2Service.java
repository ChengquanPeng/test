package com.stable.service.model.prd;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.model.RunModelService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.StringUtil;
import com.stable.utils.TagUtil;
import com.stable.vo.QiBaoInfo;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class Qibao2Service {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RunModelService runModelService;

//	@javax.annotation.PostConstruct
	public void test() {
//		 大旗形
//		 002612-20200529
//		 002900-20210315
//		 600789-20200115
//		 000678-20220610
//		 000025-20220519
//		 000017-20220526
//		 600798-20201113
//		 600187-002144
		// 小旗形
//	 	 000582-20220331
		// 大旗形
//		String[] codes = { "002612", "002900", "600789", "000678", "000025", "000017", "600798" };
//		int[] dates = { 20200529, 20210315, 20200115, 20220610, 20220519, 20220526, 20201113 };
		// 小旗形
//		String[] codes = { "000582", "000563", "601515" };
//		int[] dates = { 20220331, 20220701, 20220701 };
//		 十字星
//		String[] codes = { "002752", "000498", "601117" };
//		int[] dates = { 20211115, 20220105, 20210608 };
		String[] codes = { "002612", "002900", "600789", "000678", "000025", "000017", "600798", "000582", "000563",
				"601515", "000755" };
		int[] dates = { 20200529, 20210315, 20200115, 20220610, 20220519, 20220526, 20201113, 20220331, 20220701,
				20220701, 20220705 };
		for (int i = 0; i < codes.length; i++) {
			String code = codes[i];
			int date = dates[i];

			CodeBaseModel2 newOne = new CodeBaseModel2();
			newOne.setZfjjup(2);
			newOne.setZfjjupStable(1);
			newOne.setCode(code);
			newOne.setPls(1);
			System.out.println("==========" + stockBasicService.getCodeName2(code) + "==========");
			qibao(date, newOne, true, new StringBuffer());
			if (newOne.getDibuQixingV2() > 0 || newOne.getDibuQixingV22() > 0) {
				System.err.println(stockBasicService.getCodeName2(code) + "=====" + ",大旗形:" + newOne.getDibuQixingV2()
						+ ",小旗形:" + newOne.getDibuQixingV22());
			} else {
				System.out.println(stockBasicService.getCodeName2(code) + "=====" + ",大旗形:" + newOne.getDibuQixingV2()
						+ ",小旗形:" + newOne.getDibuQixingV22());
			}
		}
		System.exit(0);
	}

	/** 起爆 */
	public void qibao(int date, CodeBaseModel2 newOne, boolean isSamll, StringBuffer qx) {
		if (runModelService.stTuiShi(newOne)) {
			setQxRes(newOne, true, true);
			newOne.setZyxingt(0);
			return;
		}
		/** 大小旗形 */
		qx(date, newOne, isSamll, qx);

		/** 是否在起爆位 */
		// TODO
//		if (newOne.getDibuQixingV2() > 0 || newOne.getDibuQixingV22() > 0 || newOne.getZyxing() > 0) {
//			newOne.setQb(1);
//		} else {
//			newOne.setQb(0);
//		}
	}

	private void qx(int date, CodeBaseModel2 newOne, boolean isSamll, StringBuffer qx) {
		if (newOne.getPls() == 2 || !TagUtil.isDibuSmall(isSamll, newOne)) {// 排除的和大票大票不用check
			if (newOne.getPls() == 1 || newOne.getShooting11() == 1 || (newOne.getZfjjup() >= 4
					&& newOne.getFinOK() >= 1 && isSamll && newOne.getHolderNumT3() >= 50)) {// 人工的需要check||底部优质大票||一些底部小涨-stable0有业绩的小票(热点票)
			} else {
				setQxRes(newOne, true, true);
				return;
			}
		}
		/** 起爆点,底部旗形1：大旗形 **/
		qx1(date, newOne, qx);
		if (newOne.getDibuQixingV2() == 0) {
			/** 起爆点,底部旗形2：小旗形 **/
			qx2(date, newOne, qx);
		}
	}

	/** 起爆点,底部旗形1：大旗形 **/
	private void qx1(int date, CodeBaseModel2 newOne, StringBuffer qx) {
		List<TradeHistInfoDaliy> list = null;
		if (newOne.getDibuQixingV2() == 0) {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
					EsQueryPageUtil.queryPage30, SortOrder.DESC);
		} else {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), newOne.getQixing(), date,
					EsQueryPageUtil.queryPage9999, SortOrder.DESC);
		}
		QiBaoInfo res = null;
		for (int i = 1; i < list.size(); i++) {
			TradeHistInfoDaliy chk = list.get(i);
			if (chk.getTodayChangeRate() >= 8.0 && i >= 3) {
				res = isQixingType1(chk, list);
				if (res != null) {// 1.是否旗形
					break;
				}
			}
		}
		boolean dibq1 = dibuPreChk(newOne, res);// 2.是否底部旗形(旗形过滤)：1.在底部旗形，2.旗形前没怎么涨,3.是否更优的旗形:进一步判断底部旗形
		if (dibq1) {
			if (newOne.getDibuQixingV2() == 0) {
				qx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
			}
			newOne.setDibuQixingV2(res.getDate());
		} else {
			setQxRes(newOne, true, false);
		}
	}

	/** 起爆点,底部旗形2：小旗形 **/
	private void qx2(int date, CodeBaseModel2 newOne, StringBuffer qx) {
		List<TradeHistInfoDaliy> list = null;
		if (newOne.getDibuQixingV22() == 0) {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
					EsQueryPageUtil.queryPage30, SortOrder.DESC);
		} else {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), newOne.getDibuQixingV22(),
					date, EsQueryPageUtil.queryPage9999, SortOrder.DESC);
		}

		TradeHistInfoDaliy d2tian = list.get(0);
		double d2tianChange = 0.0;
		QiBaoInfo res = null;
		for (int i = 1; i < list.size(); i++) {
			TradeHistInfoDaliy chk = list.get(i);
			d2tianChange = d2tian.getTodayChangeRate();

			// 1.两天都是上涨的
			// 2.两天上涨超7个点
			// 3.或者两天上涨累计超4.5 && 整幅超9.5
			if (i >= 5 && d2tianChange > 0 && chk.getTodayChangeRate() > 0
					&& (((d2tianChange + chk.getTodayChangeRate()) >= 7.0)// 2.两天上涨超7个点
							// 3.或者两天上涨累计超4.5 && 整幅超9.5
							|| ((d2tianChange + chk.getTodayChangeRate()) > 4.5
									&& CurrencyUitl.cutProfit(chk.getLow(), d2tian.getHigh()) > 9.5))) {
				res = isQixingType2(chk, d2tian, list);
				if (res != null) {// 1.是否旗形
					break;
				}
			}
			d2tian = chk;
		}

		boolean dibq2 = dibuPreChk(newOne, res);// 2.是否底部旗形(旗形过滤)：1.在底部旗形，2.旗形前没怎么涨,3.是否更优的旗形:进一步判断底部旗形
		if (dibq2) {
			if (newOne.getDibuQixingV22() == 0) {
				qx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
			}
			newOne.setDibuQixingV22(res.getDate());
		} else {
			setQxRes(newOne, false, true);
		}
	}

	// CHK Date 之前的验证
	private boolean dibuPreChk(CodeBaseModel2 newOne, QiBaoInfo res) {
		if (res == null) {
			return false;
		}
		// 排除1:拉高后的旗形：旗形日前<1>个月涨幅低于15%
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0,
				res.getDate(), EsQueryPageUtil.queryPage45, SortOrder.DESC);
		double low = Integer.MAX_VALUE;
		for (int i = 1; i <= 20; i++) {
			TradeHistInfoDaliy nf = list.get(i);
			if (nf.getClosed() < low) {
				low = nf.getClosed();// 最小
			}
		}
		if (low < res.getYesterdayPrice()) {
			if (CurrencyUitl.cutProfit(low, res.getYesterdayPrice()) > res.getChkRate()) {// 本身已经大涨了，之前就不能涨太多
				log.info("本身已经大涨了，之前就不能涨太多1");
				return false;// 涨幅太大
			}
		}
		// 排除2:拉高后的旗形：旗形日前<2>个月涨幅低于20%
		low = Integer.MAX_VALUE;
		for (TradeHistInfoDaliy nf : list) {
			if (nf.getClosed() < low) {
				low = nf.getClosed();// 最小
			}
		}
		if (low < res.getYesterdayPrice()) {
			if (CurrencyUitl.cutProfit(low, res.getYesterdayPrice()) > 20) {// 本身已经大涨了，之前就不能涨太多
				log.info("本身已经大涨了，之前就不能涨太多2");
				return false;// 涨幅太大
			}
		}

		// 排除2：半年内已经拉高过的
		// 1.之前没有《连续10个交易日》涨幅超过35%的
		// 1.之前没有《连续20个交易日》涨幅超过50%的
		List<TradeHistInfoDaliy> list2 = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0,
				res.getDate(), EsQueryPageUtil.queryPage120, SortOrder.DESC);
		List<TradeHistInfoDaliy> tmpl = new LinkedList<TradeHistInfoDaliy>();
		List<TradeHistInfoDaliy> tmpl2 = new LinkedList<TradeHistInfoDaliy>();
		for (int i = list2.size() - 1; i >= 0; i--) {

			// 10个交易日超过35%的
			TradeHistInfoDaliy t = list2.get(i);
			if (tmpl2.size() <= 10) {
				tmpl2.add(t);
			}
			if (tmpl2.size() > 10) {
				tmpl2.remove(0);
			}
			if (tmpl2.size() == 10) {
				TradeHistInfoDaliy topDate = tmpl2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
						.get();
				TradeHistInfoDaliy lowDate = tmpl2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow))
						.get();
				// 上涨趋势
				if (topDate.getDate() > lowDate.getDate()) {
					if (CurrencyUitl.cutProfit(lowDate.getLow(), topDate.getHigh()) >= 35) {
						// 有的是K线是挖坑，有的是拉高，拉高收货回踩后的旗形可能会错过，比如002864
						log.info("10个交易日超过35%," + lowDate.getDate() + "-" + topDate.getDate());
						return false;
					}
				}
			}

			// 20个交易日超过50%的
			if (tmpl.size() <= 20) {
				tmpl.add(t);
			}
			if (tmpl.size() > 20) {
				tmpl.remove(0);
			}
			if (tmpl.size() == 20) {
				TradeHistInfoDaliy topDate = tmpl.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh))
						.get();
				TradeHistInfoDaliy lowDate = tmpl.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow))
						.get();
				// 上涨趋势
				if (topDate.getDate() > lowDate.getDate()) {
					if (CurrencyUitl.cutProfit(lowDate.getLow(), topDate.getHigh()) >= 50) {
						log.info("20个交易日超过50%");
						return false;
					}
				}
			}
		}

		// 前10个交易
		for (int i = 1; i <= 5; i++) {// 排除大涨的哪天
			TradeHistInfoDaliy nf = list.get(i);
			if (-5.0 > nf.getTodayChangeRate() || nf.getTodayChangeRate() >= 3.5) {// 大涨直接排除
				log.info("之前不能大涨大跌");
				return false;
			}
			// 上涨趋势前上涨，量大于这天，不要
			if (nf.getLow() < res.getLow() && nf.getTodayChangeRate() > 0 && nf.getVolume() > res.getVol()) {
				log.info("之前已经上涨放量");
				return false;
			}
		}
		return true;
	}

	/**
	 * chkdate之后的验证旗形1-大阳
	 */
	private QiBaoInfo isQixingType1(TradeHistInfoDaliy chk, List<TradeHistInfoDaliy> list) {
		// log.info("=====chk:" + chk.getDate());
		List<TradeHistInfoDaliy> tmp = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > chk.getDate()) {
				tmp.add(0, nf);// 改为正序循环
				if (nf.getClosed() < chk.getYesterdayPrice()) {// 单阳不破:已经破掉
					log.info(chk.getDate() + "=====>单阳不破:已经破掉" + nf.getDate());
					return null;
				}
			}
		}
		if (tmp.size() >= 3) {// 至少大涨后有三天数据
			TradeHistInfoDaliy d2tian = tmp.get(0);
			// 如果第二天涨了，是十字星或者高开低走
			if (d2tian.getTodayChangeRate() > 0) {
				if (d2tian.getClosed() > d2tian.getOpen()) {// 正常收红了
					// 不是十字星或不是上影线
					if (!LineAvgPrice.isShangYingXian(d2tian)
							&& CurrencyUitl.cutProfit(d2tian.getOpen(), d2tian.getClosed()) > 0.5) {

//						System.err.println(chk.getDate());
//						System.err.println(tmp.get(1).getDate());
//						System.err.println(tmp.get(1).getYesterdayPrice());
//						System.err.println(tmp.get(1).getYesterdayPrice() * 1.01);
//						System.err.println(tmp.get(1).getHigh());
//						System.err.println((tmp.get(1).getYesterdayPrice() * 1.01) >= tmp.get(1).getHigh());

						if (d2tian.getTodayChangeRate() >= 9.5 && tmp.get(1).getTodayChangeRate() <= -6.5
								&& ((tmp.get(1).getYesterdayPrice() * 1.01) >= tmp.get(1).getHigh())) {
							// 2天大涨&涨停，第三天大阴线
//							log.info("d2tian=" + d2tian.getDate());
							if (tmp.size() >= 4) {
								double t0 = tmp.get(1).getTodayChangeRate() + tmp.get(2).getTodayChangeRate()
										+ tmp.get(3).getTodayChangeRate();
								if (t0 <= -13.5) {
									// 3天下跌13.5%以上
								} else {
									log.info("=====>非十字星或上影线1");
									return null;
								}
							} else {
								log.info("=====>非十字星或上影线2");
								return null;
							}
						} else {
							log.info("=====>非十字星或上影线3");
							return null;
						}
					}

				} // else 高开低走
			}
			// 收阴线
			boolean d2tianHigh = (chk.getHigh() <= d2tian.getHigh());
			double high = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
			if (isOk(high, tmp, d2tianHigh)) {
				QiBaoInfo qi = new QiBaoInfo();
				qi.setDate(chk.getDate());
				qi.setPrice(high);
				qi.setVol((chk.getVolume() > d2tian.getVolume()) ? chk.getVolume() : d2tian.getVolume());
				qi.setYesterdayPrice(chk.getYesterdayPrice());
				qi.setLow(chk.getLow());
				return qi;
			} else {
				return null;
			}
		}
		log.info(chk.getCode() + "=====>默认false V2-Da," + chk.getDate());
		return null;
	}

	/**
	 * 旗形2-大阳
	 */
	private QiBaoInfo isQixingType2(TradeHistInfoDaliy chk, TradeHistInfoDaliy d2tian, List<TradeHistInfoDaliy> list) {
		// log.info("=====chk:" + chk.getDate());
		List<TradeHistInfoDaliy> tmp = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > d2tian.getDate()) {
				tmp.add(0, nf);// 改为正序循环
				if (nf.getClosed() < chk.getYesterdayPrice()) {// 单阳不破:已经破掉
					log.info("=====>单阳不破:已经破掉(只能破一次)");
					return null;
				}
			}
		}
		if (tmp.size() >= 3) {// 至少大涨后有三天数据
			// 收阴线
			double high = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
			if (isOk(high, tmp, false)) {
				QiBaoInfo qi = new QiBaoInfo();
				qi.setDate(chk.getDate());
				qi.setPrice(high);
				qi.setVol((chk.getVolume() > d2tian.getVolume()) ? chk.getVolume() : d2tian.getVolume());
				qi.setYesterdayPrice(chk.getYesterdayPrice());
				qi.setLow(chk.getLow());
				return qi;
			} else {
				return null;
			}
		}
		log.info(chk.getCode() + "=====>默认false V2-x," + chk.getDate());
		return null;
	}

	// 旗形：后续的都不能超过之前高点(只允许一次)
	private boolean isOk(double high, List<TradeHistInfoDaliy> tmp, boolean d2tianHigh) {
		for (TradeHistInfoDaliy t : tmp) {
			if (t.getHigh() > high) {
				log.info(t.getCode() + "=====>超过最高," + t.getDate() + "," + t.getHigh());
				return false;
			}
		}
		boolean ok = false;
		// 至少2天下跌
		if (d2tianHigh) {
			TradeHistInfoDaliy d1 = tmp.get(0);
			if (d1.getTodayChangeRate() > 0 && d1.getClosed() > d1.getOpen()) {
				// 不含当天
				if (tmp.get(1).getTodayChangeRate() < 0.0 && tmp.get(2).getTodayChangeRate() < 0.0
						&& tmp.get(1).getVolume() > tmp.get(2).getVolume()) {
					ok = true;
				}
			} else {
				// 包含当天
				if (tmp.get(1).getTodayChangeRate() < 0.0 && d1.getVolume() > tmp.get(1).getVolume()) {
					ok = true;
				}
			}
		} else {
			if (tmp.get(0).getTodayChangeRate() < 0.0 && tmp.get(1).getTodayChangeRate() < 0.0
					&& tmp.get(0).getVolume() > tmp.get(1).getVolume()) {
				ok = true;
			}
		}
		log.info(tmp.get(0).getCode() + "=====>没有2天下跌");
		return ok;
	}

	private void setQxRes(CodeBaseModel2 newOne, boolean isQx1, boolean isQx2) {
		if (isQx1) {
			if (newOne.getDibuQixingV2() > 0) {
				String jsHist = newOne.getDibuQixingV2() + "大旗形" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setDibuQixingV2(0);
		}
		if (isQx2) {
			if (newOne.getDibuQixingV22() > 0) {
				String jsHist = newOne.getDibuQixingV22() + "小旗形" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setDibuQixingV22(0);
		}
	}
}
