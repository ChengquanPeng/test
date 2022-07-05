package com.stable.service.model;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.StringUtil;
import com.stable.vo.QiBaoInfo;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class QibaoService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;

//	@javax.annotation.PostConstruct
//	public void test() {
//		 大旗形
//		 002612-20200529
//		 002900-20210315
//		 600789-20200115
//		 000678-20220610
//		 000025-20220519
//		 000017-20220526
//		 600798-20201113
//		小旗形
//	 	 000582-20220331
	// 大旗形
//		String[] codes = { "002612", "002900", "600789", "000678", "000025", "000017", "600798" };
//		int[] dates = { 20200529, 20210315, 20200115, 20220610, 20220519, 20220526, 20201113 };
	// 小旗形
//		String[] codes = { "000582", "000563", "601515" };
//		int[] dates = { 20220331, 20220701, 20220701 };
	// 十字星
//		String[] codes = { "002752" };
//		int[] dates = { 20211115 };
////
//		String[] codes = { "000582" };
//		int[] dates = { 20220331 };
//
//		for (int i = 0; i < codes.length; i++) {
//			String code = codes[i];
//			int date = dates[i];
//
//			CodeBaseModel2 newOne = new CodeBaseModel2();
//			newOne.setZfjjup(2);
//			newOne.setZfjjupStable(1);
//			newOne.setCode(code);
//			MonitorPoolTemp pool = new MonitorPoolTemp();
//			qibao(date, newOne, pool, true, new StringBuffer(), new StringBuffer());
//			System.err.println(code + "=====" + "Qixing:" + newOne.getQixing() + ",大旗形:" + newOne.getDibuQixing()
//					+ ",小旗形:" + newOne.getDibuQixing2() + ",Zyxing:" + newOne.getZyxing() + ",EX:"
//					+ newOne.getQixingStr());
//			System.err.println(pool);
//		}
//		System.exit(0);
//	}

	/** 起爆 */
	public void qibao(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer qx,
			StringBuffer szx) {
		/** 大小旗形 */
		qx(date, newOne, pool, isSamll, qx);
		/** 中阳十字星 */
		szx(date, newOne, pool, isSamll, szx);

		/** 是否在起爆位 */
		if (newOne.getDibuQixing() > 0 || newOne.getDibuQixing2() > 0 || newOne.getZyxing() > 0) {
			newOne.setQb(1);
		} else {
			newOne.setQb(0);
		}
	}

	private void qx(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer qx) {
		if (newOne.getPls() == 2 || !codeModelService.isDibuSmall(isSamll, newOne)) {// 排除的和大票大票不用check
			if (newOne.getPls() == 1) {// 人工的需要check
			} else {
				setQxRes(newOne, pool, true, true);
				return;
			}
		}
		/** 起爆点,底部旗形1：大旗形 **/
		qx1(date, newOne, pool, qx);
		if (newOne.getDibuQixing() == 0) {
			/** 起爆点,底部旗形2：小旗形 **/
			qx2(date, newOne, pool, qx);
		}
	}

	/** 起爆点,底部旗形1：大旗形 **/
	private void qx1(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, StringBuffer qx) {
		List<TradeHistInfoDaliy> list = null;
		if (newOne.getDibuQixing() == 0) {
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
				res = isQixingType1(chk, list, pool);
				if (res != null) {// 1.是否旗形
					break;
				}
			}
		}
		boolean dibq1 = dibuPreChk(newOne, res, true);// 2.是否底部旗形(旗形过滤)：1.在底部旗形，2.旗形前没怎么涨,3.是否更优的旗形:进一步判断底部旗形
		if (dibq1) {
			if (newOne.getDibuQixing() == 0) {
				qx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
			}
			pool.setShotPointDate(res.getDate());
			newOne.setQixingStr(res.ex());
			newOne.setQixing(res.getDate());
			newOne.setDibuQixing(res.getDate());
		} else {
			setQxRes(newOne, pool, true, false);
		}
	}

	/** 起爆点,底部旗形2：小旗形 **/
	private void qx2(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, StringBuffer qx) {
		List<TradeHistInfoDaliy> list = null;
		if (newOne.getDibuQixing2() == 0) {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
					EsQueryPageUtil.queryPage30, SortOrder.DESC);
		} else {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), newOne.getDibuQixing2(), date,
					EsQueryPageUtil.queryPage9999, SortOrder.DESC);
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
				res = isQixingType2(chk, d2tian, list, pool);
				if (res != null) {// 1.是否旗形
					break;
				}
			}
			d2tian = chk;
		}

		boolean dibq2 = dibuPreChk(newOne, res, true);// 2.是否底部旗形(旗形过滤)：1.在底部旗形，2.旗形前没怎么涨,3.是否更优的旗形:进一步判断底部旗形
		if (dibq2) {
			if (newOne.getDibuQixing2() == 0) {
				qx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
			}
			pool.setShotPointDate(res.getDate());
			newOne.setQixingStr(res.ex());
			newOne.setQixing(res.getDate());
			newOne.setDibuQixing2(res.getDate());
		} else {
			setQxRes(newOne, pool, false, true);
		}
	}

	private void szx(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer szx) {
		if (newOne.getPls() == 2 || !codeModelService.isDibuSmall(isSamll, newOne)) {
			if (newOne.getPls() == 1) {// 人工的需要check
			} else {
				newOne.setZyxing(0);
				return;
			}
		}
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);
		// 起爆点：中阳线,第二天十字星或者小影线
		// 1. 第一天中阳线,3-6个点,,第二天十字星或者小影线
		// 2.前5天涨幅低于2%
		TradeHistInfoDaliy d2tian = list.get(0);
		QiBaoInfo res = null;
		double moniHigh = 0.0;
		for (int i = 1; i < list.size(); i++) {
			TradeHistInfoDaliy chk = list.get(i);
			if (d2tian.getOpen() >= d2tian.getClosed()
					|| CurrencyUitl.cutProfit(d2tian.getOpen(), d2tian.getClosed()) <= 0.5) {// 第二天十字星或者小影线
				// 收影线或者10字星
				if (3.0 <= chk.getTodayChangeRate() && chk.getTodayChangeRate() <= 7.0) {// 第一天中阳线,3-6个点
					if (isShizixing(newOne.getCode(), chk.getDate())) {
						res = new QiBaoInfo();
						res.setDate(chk.getDate());
						res.setYesterdayPrice(chk.getYesterdayPrice());
						res.setPrice(chk.getHigh());
						moniHigh = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
						break;
					}
				}
			}
			d2tian = chk;
		}

		boolean isOk = false;
		if (res != null) {
			// chkdate之后的验证
			boolean preChkOk = true;
			int tims = 0;
			for (TradeHistInfoDaliy nf : list) {// 倒序循环
				if (nf.getDate() > res.getDate()) {
					if (nf.getClosed() < res.getYesterdayPrice()) {// 单阳不破:已经破掉
						tims++;
						if (tims > 1) {
							log.info(res.getDate() + " zyx=====>单阳不破:已经破掉(只能破一次)" + nf.getDate());
							preChkOk = false;
							break;
						}
					}
					if (nf.getClosed() > res.getPrice()) {// CHK 最高价
						preChkOk = false;
						break;
					}
				}
			}
			// chkdate 之前的验证
			if (preChkOk) {
				preChkOk = this.dibuPreChk(newOne, res, false);
			}
			isOk = preChkOk;
		}

		if (isOk) {
			if (newOne.getZyxing() == 0) {
				szx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
			}
			newOne.setZyxing(res.getDate());
			pool.setShotPointPriceSzx(moniHigh);
		} else {
			if (newOne.getZyxing() > 0) {
				String jsHist = newOne.getZyxing() + "十字星" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setZyxing(0);
			pool.setShotPointPriceSzx(0);
		}
	}

	private void setQxRes(CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isQx1, boolean isQx2) {
		if (isQx1) {
			if (newOne.getDibuQixing() > 0) {
				String jsHist = newOne.getDibuQixing() + "大旗形" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setDibuQixing(0);
		}
		if (isQx2) {
			if (newOne.getDibuQixing2() > 0) {
				String jsHist = newOne.getDibuQixing2() + "小旗形" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setDibuQixing2(0);
		}
		if (newOne.getDibuQixing() == 0 && newOne.getDibuQixing2() == 0) {
			newOne.setQixing(0);
			newOne.setQixingStr("");
			pool.setShotPointDate(0);
			pool.setShotPointPrice(0);
			pool.setShotPointPriceLow(0);
			pool.setShotPointPriceLow5(0);
		}
	}

	// CHK Date 之前的验证
	private boolean dibuPreChk(CodeBaseModel2 newOne, QiBaoInfo res, boolean isQx) {
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

		// 排除3:排除退市股票&ST
		if (stTuiShi(newOne)) {
			return false;
		}

		if (isQx) {
			// 前10个交易
			int up4 = 0;
			for (int i = 1; i <= 10; i++) {// 排除大涨的哪天
				TradeHistInfoDaliy nf = list.get(i);
				if (nf.getTodayChangeRate() >= 7.5) {// 大涨直接排除
					log.info("之前已经大涨7%");
					return false;
				}
				// 上涨趋势前上涨，量大于这天，不要
				if (nf.getLow() < res.getLow() && nf.getTodayChangeRate() > 0 && nf.getVolume() > res.getVol()) {
					log.info("之前已经上涨放量");
					return false;
				}
				if (nf.getTodayChangeRate() >= 4) {// 涨幅超过4个点，++
					up4++;
				}
			}
			// 前面几个交易日波动必须小于4个点（只允许1-2次）
			if (up4 <= 1) {
				return true;
			}
			log.info("前面几个交易日波动4%超1");
			return false;
		} else {
			return true;
		}
	}

	// 排除3:排除退市股票&ST
	private boolean stTuiShi(CodeBaseModel2 newOne) {
		String name = stockBasicService.getCodeName(newOne.getCode());
		if (name.startsWith(Constant.TUI_SHI) || name.endsWith(Constant.TUI_SHI) || name.contains("ST")) {
			log.info("退市");
			return true;
		}
		return false;
	}

	/**
	 * 十字星
	 */
	private boolean isShizixing(String code, int date) {
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage45, SortOrder.DESC);
		// 2. 5天涨幅低于4.5%
		for (int i = 1; i < 6; i++) {
			TradeHistInfoDaliy t = list.get(i);
			if (t.getTodayChangeRate() > 4.5) {
				return false;
			}
		}
		// 3. 2个月之内有大上影线
		for (int i = 1; i < list.size(); i++) {
			TradeHistInfoDaliy t = list.get(i);
			if (t.getTodayChangeRate() > 3 && LineAvgPrice.isShangYingXian(t)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * chkdate之后的验证旗形1-大阳
	 */
	private QiBaoInfo isQixingType1(TradeHistInfoDaliy chk, List<TradeHistInfoDaliy> list, MonitorPoolTemp pool) {
		// log.info("=====chk:" + chk.getDate());
		List<TradeHistInfoDaliy> tmp = new LinkedList<TradeHistInfoDaliy>();
		int tims = 0;
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > chk.getDate()) {
				tmp.add(0, nf);// 改为正序循环
				if (nf.getClosed() < chk.getYesterdayPrice()) {// 单阳不破:已经破掉
					tims++;
					if (tims > 1) {
						log.info(chk.getDate() + "=====>单阳不破:已经破掉(只能破一次)" + nf.getDate());
						return null;
					}
				}
			}
		}
		boolean chkrateupdate = false;
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
									chkrateupdate = true;// 之前涨幅不能超过默认15,这个宽幅震荡可以20
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
			double high = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
			if (isOk(high, tmp)) {
				pool.setShotPointPrice(high);
				pool.setShotPointPriceLow(CurrencyUitl.roundHalfUp(chk.getYesterdayPrice() * 1.01));// 旗形底部预警1%
				pool.setShotPointPriceLow5(CurrencyUitl.roundHalfUp(chk.getYesterdayPrice() * 1.05));// 旗形底部预警5%

				QiBaoInfo qi = new QiBaoInfo();
				qi.setDate(chk.getDate());
				qi.setPrice(high);
				qi.setVol((chk.getVolume() > d2tian.getVolume()) ? chk.getVolume() : d2tian.getVolume());
				qi.setYesterdayPrice(chk.getYesterdayPrice());
				qi.setLow(chk.getLow());
				if (chkrateupdate) {
					qi.setChkRate(20);
				}
				qxrange(qi, tmp);
				return qi;
			}
		}
		log.info("=====>默认false");
		return null;

	}

	private void qxrange(QiBaoInfo qi, List<TradeHistInfoDaliy> tmp) {
		System.err.println("check rate:" + tmp.get(1).getDate() + "-" + tmp.get(tmp.size() - 1).getDate());
		for (int i = 1; i < tmp.size(); i++) {
			TradeHistInfoDaliy td = tmp.get(i);
			if (LineAvgPrice.isShangYingXian(td) && CurrencyUitl.cutProfit(td.getLow(), td.getHigh()) >= 4.5) {
				System.err.println("syx:" + td);
				qi.setSyx(1);
			}
			if (LineAvgPrice.isDaYingxian(td)) {
				System.err.println("dyx:" + td);
				qi.setDyx(1);
			}
		}
	}

	/**
	 * 旗形2-大阳
	 */
	private QiBaoInfo isQixingType2(TradeHistInfoDaliy chk, TradeHistInfoDaliy d2tian, List<TradeHistInfoDaliy> list,
			MonitorPoolTemp pool) {
		// log.info("=====chk:" + chk.getDate());
		List<TradeHistInfoDaliy> tmp = new LinkedList<TradeHistInfoDaliy>();
		int tims = 0;
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > chk.getDate()) {
				tmp.add(0, nf);// 改为正序循环
				if (nf.getClosed() < chk.getYesterdayPrice()) {// 单阳不破:已经破掉
					tims++;
					if (tims > 1) {
						log.info("=====>单阳不破:已经破掉(只能破一次)");
						return null;
					}
				}
			}
		}
		if (tmp.size() >= 4) {// 至少大涨后有三天数据
			// 收阴线
			double high = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
			if (isOk(high, tmp)) {
				pool.setShotPointPrice(high);
				pool.setShotPointPriceLow(CurrencyUitl.roundHalfUp(chk.getYesterdayPrice() * 1.01));// 旗形底部预警1%
				pool.setShotPointPriceLow5(CurrencyUitl.roundHalfUp(chk.getYesterdayPrice() * 1.05));// 旗形底部预警5%

				QiBaoInfo qi = new QiBaoInfo();
				qi.setDate(chk.getDate());
				qi.setPrice(high);
				qi.setVol((chk.getVolume() > d2tian.getVolume()) ? chk.getVolume() : d2tian.getVolume());
				qi.setYesterdayPrice(chk.getYesterdayPrice());
				qi.setLow(chk.getLow());

				qxrange(qi, tmp);
				return qi;
			}
		}
		log.info("=====>默认false");
		return null;
	}

	// 旗形：后续的都不能超过之前高点(只允许一次)
	private boolean isOk(double high, List<TradeHistInfoDaliy> tmp) {
		int tims = 0;
		for (TradeHistInfoDaliy t : tmp) {
			if (t.getHigh() > high) {
				tims++;
				if (tims > 1) {
					log.info("=====>超过最高");
					return false;
				}
			}
		}
		if (tims <= 1) {
			return true;
		}
		log.info("=====>超过最高2");
		return false;
	}

}
