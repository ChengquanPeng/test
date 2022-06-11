package com.stable.service.model;

import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class QibaoService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

//	@javax.annotation.PostConstruct
	public void test() {
//		String code = "002612";
//		int date = 20210208 ,20210209, 20210223 20210315
//		String code = "002612";
//		int date = 20200526;
//		String code = "603797";
//		int date = 20220611;
		System.err.println("=====");
		System.err.println("=====");
		System.exit(0);
	}

	public void qibao(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool) {
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
				if (isQixing(first, nf, list)) {
					res = nf;
					break;
				}
			}
		}
		if (res != null) {
			newOne.setQixing(res.getDate());
			pool.setShotPointPrice(res.getHigh());
		} else {
			newOne.setQixing(0);
			pool.setShotPointPrice(0);
		}

		// 起爆点：中阳线,第二天十字星或者小影线
		// 1. 3-6个点
		// 2. 5天涨幅低于2%
		newOne.setZyxing(0);
		if (first.getOpen() >= first.getClosed() || CurrencyUitl.cutProfit(first.getOpen(), first.getClosed()) <= 0.5) {
			// 收影线或者10字星
			TradeHistInfoDaliy nd2 = list.get(1);
			if (3.0 <= nd2.getTodayChangeRate() && nd2.getTodayChangeRate() <= 8.0) {// 1. 3-6个点
				if (isShizixing(newOne.getCode(), nd2.getDate())) {
					newOne.setZyxing(1);
					if (pool.getShotPointPrice() == 0) {
						pool.setShotPointPrice(first.getHigh());
					}
				}
			}
		}
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
	private boolean isQixing(TradeHistInfoDaliy first, TradeHistInfoDaliy chk, List<TradeHistInfoDaliy> list) {
		if (first.getDate() == chk.getDate()) {
			return false;
		}
		// System.err.println("=====chk:" + chk.getDate());
		List<TradeHistInfoDaliy> tmp = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > chk.getDate()) {
				tmp.add(0, nf);// 改为正序循环
				if (nf.getClosed() < chk.getLow()) {// 单阳不破:已经破掉
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
			return isOk(high, tmp);
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
