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

	public void sort1ModeChk(CodeBaseModel2 cbm, MonitorPool mp, int date, StringBuffer shootNotice5) {
		if (cbm.getShooting5() <= 0) {
			double maxPrice = this.is30DayTodayPriceOk(cbm.getCode(), date);
			if (maxPrice > 0.0) {
				cbm.setShooting5(DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), 30)));// 30天,一定要尽快新高
				mp.setUpPrice(maxPrice);
				if (mp.getMonitor() == MonitorType.NO.getCode()) {
					mp.setMonitor(MonitorType.SORT1.getCode());
					mp.setRealtime(1);
				}
				shootNotice5.append(stockBasicService.getCodeName2(cbm.getCode())).append(",");
			}
		} else if (date >= cbm.getShooting5()) {
			cbm.setShooting5(0);
			if (mp.getMonitor() == MonitorType.SORT1.getCode()) {
				mp.setMonitor(MonitorType.NO.getCode());
				mp.setRealtime(0);
				mp.setUpPrice(0);
			}
		}
	}

	/**
	 * 1.30个交易日内振幅超80%的涨幅
	 */
	private double is30DayTodayPriceOk(String code, int date) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);

		double maxPrice = l2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getHigh)).get().getHigh();
		double minPrice = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getLow)).get().getLow();

		// log.info("{} {} ", minPrice, maxPrice);

		double persent3 = CurrencyUitl.cutProfit(minPrice, maxPrice);
		if (persent3 >= 80.0) {
			return maxPrice;
		}
		return 0.0;
	}
}
