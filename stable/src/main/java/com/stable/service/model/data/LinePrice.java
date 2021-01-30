package com.stable.service.model.data;

import java.util.Comparator;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

public class LinePrice {

	// 整幅未超过chkdouble%
	public static boolean priceCheckForMid(DaliyTradeHistroyService daliyTradeHistroyService, String code, int date,
			double chkdouble) {
		return priceCheckForMid(daliyTradeHistroyService, code, date, chkdouble, EsQueryPageUtil.queryPage250);
	}

	public static boolean priceCheckForMid(DaliyTradeHistroyService daliyTradeHistroyService, String code, int date,
			double chkdouble, EsQueryPageReq page) {
		List<TradeHistInfoDaliy> listN = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date, page,
				SortOrder.DESC);
		TradeHistInfoDaliy dmax = listN.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		TradeHistInfoDaliy dmin = listN.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
		double maxPrice = dmax.getHigh();
		double minPrice = dmin.getLow();
		double profit = CurrencyUitl.cutProfit(minPrice, maxPrice);
		if (profit > chkdouble) {
			return false;
		} else {
			return true;
		}
	}
}
