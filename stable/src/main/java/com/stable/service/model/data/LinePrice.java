package com.stable.service.model.data;

import java.util.Comparator;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.TradeHistInfoDaliy;

public class LinePrice {

	// 整幅未超过chkdouble%
	public static boolean priceCheckForMid(DaliyTradeHistroyService daliyTradeHistroyService, String code, int date,
			double chkdouble) {
		List<TradeHistInfoDaliy> listD180 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage250, SortOrder.DESC);
		TradeHistInfoDaliy dmax = listD180.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		TradeHistInfoDaliy dmin = listD180.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
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
