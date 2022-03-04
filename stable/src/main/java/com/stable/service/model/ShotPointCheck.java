package com.stable.service.model;

import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.spider.realtime.RealTime;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.ShotPoint;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

/**
 * 疑似起爆点
 *
 */
@Service
public class ShotPointCheck {

	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public ShotPoint check(String code, int tradeDate, RealTime rt) {
		ShotPoint sp = new ShotPoint();
		if (checkV1(code, tradeDate, rt)) {
			sp.setV1(1);
		}
		return sp;
	}

	// 版本V1起爆点
	private boolean checkV1(String code, int tradeDate, RealTime rt) {
		if (rt != null) {
			if (priceChkV1(rt.getYesterday(), rt.getNow())) {
				TradeHistInfoDaliyNofq preDate = daliyTradeHistroyService.queryLastNofq(code);
				return (volChkV1(preDate.getVolume(), rt.getDealNums()));
			} else {
				return false;
			}

		} else {
			List<TradeHistInfoDaliyNofq> list = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, tradeDate,
					EsQueryPageUtil.queryPage2, SortOrder.DESC);

			TradeHistInfoDaliyNofq today = list.get(0);
			if (priceChkV1(today.getYesterdayPrice(), today.getClosed())) {
				TradeHistInfoDaliyNofq preDate = list.get(1);
				return (volChkV1(preDate.getVolume(), today.getVolume()));
			} else {
				return false;
			}
		}
	}

	private boolean priceChkV1(double yest, double now) {
		return CurrencyUitl.cutProfit(yest, now) >= 3.0;
	}

	private boolean volChkV1(double yest, double now) {
		double chkVol = Double.valueOf(yest * 1.8);
		return (now >= chkVol);
	}
}
