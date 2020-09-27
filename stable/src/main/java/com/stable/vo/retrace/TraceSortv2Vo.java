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

	private DaliyBasicInfo db;
	private FinanceBaseInfo fin;
	private FinYjkb kb;
	private FinYjyg yg;

	public String toExcel() {
		actProfit = CurrencyUitl.cutProfit(buyPrice, sellPrice);
		// 代码,日期,当日涨幅,买入价,卖出价,是否盈利,盈利多少,最高收盘价,最收盘低价,最低价,市盈率(静),市盈率(TTM),流通市值(万元),量比,
		// 最新财务公告日期,营业同比增长,净利同比增长,最近财务快预告日期,营业同比增长,净利同比增长
		String msg = code + "," + date + "," + buyDayRate + "," + buyPrice + "," + sellPrice + ","
				+ (sellPrice > buyPrice ? 1 : 0) + "," + actProfit + "," + maxPrice + "," + minPrice + "," + minLowPrice
				+ "," + db.getPe() + "," + db.getPe_ttm() + "," + db.getCirc_mv() + "," + db.getVolume_ratio() + ","
				+ fin.getAnnDate() + "," + fin.getYyzsrtbzz() + "," + fin.getGsjlrtbzz() + ",";
		if (kb == null && yg == null) {
			msg += ",No,No,No";
		} else {
			if (kb != null) {
				msg += kb.getAnnDate() + "," + kb.getYyzsrtbzz() + "," + kb.getJlrtbzz();
			} else {
				msg += yg.getAnnDate() + ",No," + yg.getJlrtbzz();
			}
		}

		return msg;
	}
}