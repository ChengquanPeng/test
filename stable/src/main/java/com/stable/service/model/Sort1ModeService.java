package com.stable.service.model;

import java.util.Comparator;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.spi.req.EsQueryPageReq;

@Service
public class Sort1ModeService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	/**
	 * 1.30个交易日内振幅超的涨幅
	 */
	public double daIs30DayTodayPriceOk(String code, int date, double checkLine) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);

		TradeHistInfoDaliyNofq topDate = l2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getHigh))
				.get();
		double maxPrice = topDate.getHigh();

		TradeHistInfoDaliyNofq lowDate = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getLow))
				.get();
		double minPrice = lowDate.getLow();
		// d1.getDate() < d2.getDate():是上涨趋势。
		if (lowDate.getDate() < topDate.getDate() && CurrencyUitl.cutProfit(minPrice, maxPrice) >= checkLine) {
//			log.info("AAABBB{},tradedate={},topDate={},{} topDate={},{} ", code, date, topDate.getDate(), maxPrice,
//					lowDate.getDate(), minPrice);
			return maxPrice;
		}
		return 0.0;
	}

	/**
	 * 1.30个交易日内振幅超的涨幅
	 */
	public boolean xyIs30DayTodayPriceOk(String code, int date, double checkLine, EsQueryPageReq req) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date, req,
				SortOrder.DESC);

		TradeHistInfoDaliyNofq topDate = l2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getHigh))
				.get();
		double maxPrice = topDate.getHigh();

		TradeHistInfoDaliyNofq lowDate = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getLow))
				.get();
		double minPrice = lowDate.getLow();
		// d1.getDate() < d2.getDate():是上涨趋势。
		if (lowDate.getDate() < topDate.getDate() && CurrencyUitl.cutProfit(minPrice, maxPrice) <= checkLine) {
//			log.info("AAABBB{},tradedate={},topDate={},{} topDate={},{} ", code, date, topDate.getDate(), maxPrice,
//					lowDate.getDate(), minPrice);
			return true;
		}
		return false;
	}
}
