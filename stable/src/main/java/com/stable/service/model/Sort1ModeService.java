package com.stable.service.model;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.MonitorType;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPool;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

@Service
public class Sort1ModeService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private StockBasicService stockBasicService;

	private final double sort1checkLine = 80.0;

	public void sort1ModeChk(CodeBaseModel2 cbm, MonitorPool mp, int date, StringBuffer shootNotice5) {
		if (cbm.getShooting5() <= 0 || date > cbm.getShooting5()) {// 需要验证是否OK //或者已过期
			double maxPrice = this.daIs30DayTodayPriceOk(cbm.getCode(), date, sort1checkLine);
			if (maxPrice > 0.0) {// OK
				String name = stockBasicService.getCodeName2(cbm.getCode());
				if (!name.contains("ST")) {
					cbm.setShooting5(DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), 30)));// 30天,一定要尽快新高
					mp.setUpPrice(maxPrice);
					if (mp.getMonitor() == MonitorType.NO.getCode()) {
						mp.setMonitor(MonitorType.SORT1.getCode());
						mp.setRealtime(1);
					}
					shootNotice5.append(stockBasicService.getCodeName2(cbm.getCode())).append(",");
				} else {// NotOK
					reset(cbm, mp);
					cbm.setShooting5(DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), 180)));// 180天
				}
			} else {// NotOK
				reset(cbm, mp);
			}
		} else {
			// 一直OK，则更新价格
			if (MonitorType.SORT1.getCode() == mp.getMonitor()) {
				double maxPrice = this.daIs30DayTodayPriceOk(cbm.getCode(), date, sort1checkLine);
				if (maxPrice > 0) {
					mp.setUpPrice(maxPrice);
				} else {
					reset(cbm, mp);
				}
			}
		}
	}

	private void reset(CodeBaseModel2 cbm, MonitorPool mp) {
		cbm.setShooting5(0);
		if (mp.getMonitor() == MonitorType.SORT1.getCode()) {
			mp.setMonitor(MonitorType.NO.getCode());
			mp.setRealtime(0);
			mp.setUpPrice(0);
		}
	}

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
	public boolean xyIs30DayTodayPriceOk(String code, int date, double checkLine) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);

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
