package com.stable.vo.retrace;

import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;

import lombok.Data;

@Data
public class TraceSortv2Vo {
	private String code;
	private int date;
	private double buyPrice;
	private double sellPrice;
	private double actProfit;
	// ---
	private double maxPrice;
	private double minPrice;
	private double minLowPrice;
	private double buyDayRate;
	// ---大盘
	private double marketIndex;

	private DaliyBasicInfo basic;
	private FinanceBaseInfo fin;
	private FinYjkb kb;
	private FinYjyg yg;

	public String toExcel() {
		actProfit = CurrencyUitl.cutProfit(buyPrice, sellPrice);
		// 代码,日期,当日涨幅,当日大盘涨幅,买入价,卖出价,是否盈利,盈利多少,最高收盘价,最低收盘价,最低价幅度,最低价,最低价幅度,市盈率(静),市盈率(TTM),
		// 流通市值(万元),量比,最近财务快预告日期,营业同比增长,净利同比增长,最新财务公告日期,营业同比增长,净利同比增长,
		String msg = code + "," + date + "," + buyDayRate + "," + marketIndex + "," + buyPrice + "," + sellPrice + ","
				+ (sellPrice > buyPrice ? 1 : 0) + "," + actProfit + "," + maxPrice + "," + //
				minPrice + "," + (CurrencyUitl.cutProfit(buyPrice, minPrice)) + "," + //
				minLowPrice + "," + (CurrencyUitl.cutProfit(buyPrice, minLowPrice)) + "," + //
				basic.getPe() + "," + basic.getPe_ttm() + "," + CurrencyUitl.covertToString(basic.getCirc_mv()) + ","
				+ basic.getVolume_ratio() + ",";
		if (kb == null && yg == null) {
			msg += ",,";
		} else {
			if (kb != null) {
				msg += kb.getAnnDate() + "," + kb.getYyzsrtbzz() + "," + kb.getJlrtbzz();
			} else {
				msg += yg.getAnnDate() + ",," + yg.getJlrtbzz();
			}
		}
		if (fin == null) {
			msg += ",,,,";
		} else {
			msg += "," + fin.getAnnDate() + "," + fin.getYyzsrtbzz() + "," + fin.getGsjlrtbzz() + ",";
		}
		return msg;
	}
}