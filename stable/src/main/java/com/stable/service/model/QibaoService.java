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
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class QibaoService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;

//	@javax.annotation.PostConstruct
//	public void test() {
//		String code = "002612";
//		int date = 20210208 ,20210209, 20210223 20210315
//		String code = "002612";
//		int date = 20200526;
//		String code = "000023";
//		int date = 20220620;
//		System.err.println("=====");
//		CodeBaseModel2 newOne = new CodeBaseModel2();
//		newOne.setZfjjup(2);
//		newOne.setZfjjupStable(1);
//		newOne.setCode(code);
//		MonitorPoolTemp pool = new MonitorPoolTemp();
//		qibao(date, newOne, pool, true, new StringBuffer());
//		System.err.println("Qixing:" + newOne.getQixing());
//		System.err.println("DiQixing:" + newOne.getDibuQixing());
//		System.err.println("Zyxing:" + newOne.getZyxing());
//		System.err.println("=====");
//		System.exit(0);
//	}

	public void qibao(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll, StringBuffer qx) {
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
		TradeHistInfoDaliy res = null;
		for (TradeHistInfoDaliy nf : list) {
			if (nf.getTodayChangeRate() >= 8.0) {
				if (isQixing(first, nf, list, pool)) {// 是否旗形
					res = nf;
					break;
				}
			}
		}
		boolean isdibu = false;
		if (res != null) {
			newOne.setQixing(res.getDate());
			isdibu = dibuqixing(newOne, res, isSamll);// 旗形过滤：1.在底部旗形，2.旗形前没怎么涨
		} else {
			newOne.setQixing(0);
		}

		if (isdibu) {
			if (newOne.getDibuQixing() == 0) {
				qx.append(stockBasicService.getCodeName2(newOne.getCode())).append(",");
			}
			pool.setShotPointDate(res.getDate());
			newOne.setDibuQixing(1);
		} else {
			if (newOne.getDibuQixing() == 1) {
				String jsHist = newOne.getQixing() + "底部旗形" + ";" + newOne.getJsHist();
				newOne.setJsHist(StringUtil.subString(jsHist, 100));
			}
			newOne.setDibuQixing(0);
			pool.setShotPointDate(0);
			pool.setShotPointPrice(0);
			pool.setShotPointPriceLow(0);
			pool.setShotPointPriceLow5(0);
		}
		// 起爆点：中阳线,第二天十字星或者小影线
		// 1. 3-6个点
		// 2. 5天涨幅低于2%
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

	private boolean dibuqixing(CodeBaseModel2 newOne, TradeHistInfoDaliy res, boolean isSamll) {
		// 底部小票
		if (codeModelService.isDibuSmall(isSamll, newOne)) {

			// 排除1拉高后的旗形：旗形日前1个月涨幅低于20%
			List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0,
					res.getDate(), EsQueryPageUtil.queryPage20, SortOrder.DESC);
			double low = Integer.MAX_VALUE;
			for (TradeHistInfoDaliy nf : list) {
				if (nf.getClosed() < low) {
					low = nf.getClosed();// 最小
				}
			}
			if (low < res.getYesterdayPrice()) {
				if (CurrencyUitl.cutProfit(low, res.getYesterdayPrice()) > 20) {// 本身已经大涨了，之前就不能涨太多
					return false;// 涨幅太大
				}
			}

			// 排除2：半年内已经拉高过的旗形，之前没有连续20个交易日涨幅超过50%的
			List<TradeHistInfoDaliy> list2 = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0,
					res.getDate(), EsQueryPageUtil.queryPage120, SortOrder.DESC);
			List<TradeHistInfoDaliy> tmpl = new LinkedList<TradeHistInfoDaliy>();
			for (int i = list2.size() - 1; i >= 0; i--) {
				TradeHistInfoDaliy t = list2.get(i);
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
							return false;
						}
					}
				}
			}

			// 排除3:排除退市股票
			String name = stockBasicService.getCodeName(newOne.getCode());
			if (name.startsWith(Constant.TUI_SHI) || name.endsWith(Constant.TUI_SHI)) {
				return false;
			}
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
	 * 旗形
	 */
	private boolean isQixing(TradeHistInfoDaliy first, TradeHistInfoDaliy chk, List<TradeHistInfoDaliy> list,
			MonitorPoolTemp pool) {
		if (first.getDate() == chk.getDate()) {// 第一天不算
			// System.err.println("=====chk?:" + chk.getDate());
			return false;
		}
		// System.err.println("=====chk:" + chk.getDate());
		List<TradeHistInfoDaliy> tmp = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > chk.getDate()) {
				tmp.add(0, nf);// 改为正序循环
				if (nf.getClosed() < chk.getYesterdayPrice()) {// 单阳不破:已经破掉
					// System.err.println("=====>单阳不破:已经破掉");
					return false;
				}
			}
		}
		if (tmp.size() >= 3) {// 至少大涨后有三天数据
			TradeHistInfoDaliy d2t = tmp.get(0);
			// 如果第二天涨了，是十字星或者高开低走
			if (d2t.getTodayChangeRate() > 0) {
				if (d2t.getClosed() > d2t.getOpen()) {// 正常收红了
					// 是否十字星
					if (CurrencyUitl.cutProfit(d2t.getOpen(), d2t.getClosed()) > 0.5) {
						// System.err.println("=====>非十字星");
						return false;
					}
				} // else 高开低走
			}
			// 收阴线
			double high = (chk.getHigh() > d2t.getHigh()) ? chk.getHigh() : d2t.getHigh();
			if (isOk(high, tmp)) {
				pool.setShotPointPrice(high);
				pool.setShotPointPriceLow(CurrencyUitl.roundHalfUp(chk.getYesterdayPrice() * 1.01));// 旗形底部预警1%
				pool.setShotPointPriceLow5(CurrencyUitl.roundHalfUp(chk.getYesterdayPrice() * 1.05));// 旗形底部预警5%
				return true;
			}
		}
		// System.err.println("=====>默认false");
		return false;
	}

	// 旗形：后续的都不能超过之前高点
	private boolean isOk(double high, List<TradeHistInfoDaliy> tmp) {
		for (TradeHistInfoDaliy t : tmp) {
			if (t.getHigh() > high) {
				// System.err.println("=====>超过最高");
				return false;
			}
		}
		return true;
	}

}
