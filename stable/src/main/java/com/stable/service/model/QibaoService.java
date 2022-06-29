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
		// 002612-20200527
		// 002900-20210315
		// 600789-20200115
		// 000678-20220610
		// 000025-20220519
		// 000017-20220526
//		String[] codes = { "002612", "002900", "600789", "000678", "000025", "000017" };
//		int[] dates = { 20200527, 20210315, 20200115, 20220610, 20220519, 20220526 };
//		String[] codes = { "600695", "603991" };
//		int[] dates = { 20220628, 20220628 };
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
//			qibao(date, newOne, pool, true, new StringBuffer());
//			System.err.println(code + "=====" + "Qixing:" + newOne.getQixing() + ",DiQixing:" + newOne.getDibuQixing()
//					+ ",Zyxing:" + newOne.getZyxing());
//			System.err.println(pool);
//		}
//		System.exit(0);
//	}

	private void setQxRes(CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isQx) {
		if (newOne.getDibuQixing() == 1) {
			String jsHist = newOne.getQixing() + "底部旗形" + ";" + newOne.getJsHist();
			newOne.setJsHist(StringUtil.subString(jsHist, 100));
		}
		if (isQx) {
			newOne.setQixing(1);
		} else {
			newOne.setQixing(0);
		}
		newOne.setDibuQixing(0);
		pool.setShotPointDate(0);
		pool.setShotPointPrice(0);
		pool.setShotPointPriceLow(0);
		pool.setShotPointPriceLow5(0);
	}

	private void qx(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer qx) {
		if (newOne.getPls() == 2 || !isSamll) {
			setQxRes(newOne, pool, false);
			return;
		}
		// 起爆点1：旗形
		List<TradeHistInfoDaliy> list = null;
		if (newOne.getQixing() == 0) {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
					EsQueryPageUtil.queryPage30, SortOrder.DESC);
		} else {
			list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), newOne.getQixing(), date,
					EsQueryPageUtil.queryPage9999, SortOrder.DESC);
		}
		TradeHistInfoDaliy first = list.get(0);
		QiBaoInfo res = null;
		for (TradeHistInfoDaliy nf : list) {
			if (nf.getTodayChangeRate() >= 8.0) {
				res = isQixing(first, nf, list, pool);
				if (res != null) {// 1.是否旗形
					break;
				}
			}
		}
		List<TradeHistInfoDaliy> list2 = null;
		if (res != null) {
			list2 = dibuqixing(newOne, res, isSamll);// 2.是否底部旗形(旗形过滤)：1.在底部旗形，2.旗形前没怎么涨
		}

		if (list2 != null) {
			newOne.setQixing(res.getDate());
			boolean yzdi = dibuqixing2(list2, res);// 3.是否更优的旗形:进一步判断底部旗形
			if (yzdi) {
				if (newOne.getDibuQixing() == 0) {
					qx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
				}
				pool.setShotPointDate(res.getDate());
				newOne.setDibuQixing(1);
			} else {
				setQxRes(newOne, pool, true);
			}
		} else {
			setQxRes(newOne, pool, false);
		}
	}

	private void szx(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer qx) {
		if (newOne.getPls() == 2) {
			newOne.setZyxing(0);
			return;
		}
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);
		// 起爆点：中阳线,第二天十字星或者小影线
		// 1. 3-6个点
		// 2. 5天涨幅低于2%
		TradeHistInfoDaliy first = list.get(0);
		boolean getZyx = false;
		if (first.getOpen() >= first.getClosed() || CurrencyUitl.cutProfit(first.getOpen(), first.getClosed()) <= 0.5) {
			// 收影线或者10字星
			TradeHistInfoDaliy nd2 = list.get(1);
			if (3.0 <= nd2.getTodayChangeRate() && nd2.getTodayChangeRate() <= 7.0) {// 1. 3-6个点
				if (isShizixing(newOne.getCode(), nd2.getDate())) {
					getZyx = true;
				}
			}
		}

		if (getZyx) {
			newOne.setZyxing(1);
		} else {
			if (newOne.getZyxing() == 1) {
				String jsHist = newOne.getQixing() + "十字星" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setZyxing(0);
		}
	}

	public void qibao(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer qx) {
		qx(date, newOne, pool, isSamll, qx);
		szx(date, newOne, pool, isSamll, qx);
	}

	private boolean dibuqixing2(List<TradeHistInfoDaliy> list2, QiBaoInfo res) {
		// log.info(list2.get(list2.size() - 1).getDate());
		// log.info(list2.get(0).getDate());
		int up4 = 0;
		for (int i = 1; i <= 10; i++) {// 排除大涨的哪天
			TradeHistInfoDaliy nf = list2.get(i);
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
	}

	private List<TradeHistInfoDaliy> dibuqixing(CodeBaseModel2 newOne, QiBaoInfo res, boolean isSamll) {
		// 底部小票
		if (codeModelService.isDibuSmall(isSamll, newOne)) {
			// 排除:拉高后的旗形：旗形日前1个月涨幅低于15%
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
				if (CurrencyUitl.cutProfit(low, res.getYesterdayPrice()) > 15) {// 本身已经大涨了，之前就不能涨太多
					log.info("本身已经大涨了，之前就不能涨太多1");
					return null;// 涨幅太大
				}
			}
			// 排除:拉高后的旗形：旗形日前2个月涨幅低于20%
			low = Integer.MAX_VALUE;
			for (TradeHistInfoDaliy nf : list) {
				if (nf.getClosed() < low) {
					low = nf.getClosed();// 最小
				}
			}
			if (low < res.getYesterdayPrice()) {
				if (CurrencyUitl.cutProfit(low, res.getYesterdayPrice()) > 20) {// 本身已经大涨了，之前就不能涨太多
					log.info("本身已经大涨了，之前就不能涨太多2");
					return null;// 涨幅太大
				}
			}

			// 排除2：半年内已经拉高过的旗形，之前没有连续20个交易日涨幅超过50%的
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
					TradeHistInfoDaliy topDate = tmpl2.stream()
							.max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
					TradeHistInfoDaliy lowDate = tmpl2.stream()
							.min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
					// 上涨趋势
					if (topDate.getDate() > lowDate.getDate()) {
						if (CurrencyUitl.cutProfit(lowDate.getLow(), topDate.getHigh()) >= 35) {
							// 有的是K线是挖坑，有的是拉高，拉高收货回踩后的旗形可能会错过，比如002864
							log.info("10个交易日超过35%," + lowDate.getDate() + "-" + topDate.getDate());
							return null;
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
					TradeHistInfoDaliy topDate = tmpl.stream()
							.max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
					TradeHistInfoDaliy lowDate = tmpl.stream()
							.min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
					// 上涨趋势
					if (topDate.getDate() > lowDate.getDate()) {
						if (CurrencyUitl.cutProfit(lowDate.getLow(), topDate.getHigh()) >= 50) {
							log.info("20个交易日超过50%");
							return null;
						}
					}
				}
			}

			// 排除3:排除退市股票
			String name = stockBasicService.getCodeName(newOne.getCode());
			if (name.startsWith(Constant.TUI_SHI) || name.endsWith(Constant.TUI_SHI)) {
				log.info("退市");
				return null;
			}
			return list;
		}
		return null;
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
	 * 旗形
	 */
	private QiBaoInfo isQixing(TradeHistInfoDaliy first, TradeHistInfoDaliy chk, List<TradeHistInfoDaliy> list,
			MonitorPoolTemp pool) {
		if (first.getDate() == chk.getDate()) {// 第一天不算
			// log.info("=====chk?:" + chk.getDate());
			return null;
		}
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
		if (tmp.size() >= 3) {// 至少大涨后有三天数据
			TradeHistInfoDaliy d2tian = tmp.get(0);
			// 如果第二天涨了，是十字星或者高开低走
			if (d2tian.getTodayChangeRate() > 0) {
				if (d2tian.getClosed() > d2tian.getOpen()) {// 正常收红了
					// 不是十字星或不是上影线
					if (!LineAvgPrice.isShangYingXian(d2tian)
							&& CurrencyUitl.cutProfit(d2tian.getOpen(), d2tian.getClosed()) > 0.5) {
						log.info("=====>非十字星或上影线");
						return null;
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
