package com.stable.service.model.data;

import java.util.List;

import com.stable.constant.EsQueryPageUtil;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class LineAvgPrice {

	public static boolean isWhiteHorseForMidV2(AvgService avgService, String code, int date) {
		try {
			// 最近30条-倒序
			List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLastN(code, date,
					EsQueryPageUtil.queryPage30, true, true);
			// 是否上升趋势
			int whiteHorseTmp = 0;
			double check = clist30.get(0).getAvgPriceIndex60();// 后一个交易日
			for (int i = 0; i < clist30.size(); i++) {
				// 当天
				double c = clist30.get(i).getAvgPriceIndex60();
				if (check >= c) {// 后一个交易日大于前一个交易日，上升趋势
					whiteHorseTmp++;
				}
				check = c;
			}
			if (whiteHorseTmp >= 28) {
				return true;
			}
		} catch (Exception e) {
			log.info("code={}, date={} 计算出错.", code, date);
		}
		return false;
	}

	/**
	 * 均线排列
	 */
	public static void avgLineUp(StockBaseInfo s, CodeBaseModel2 cbm, AvgService avgService, String code, int date) {
		try {
			// 最近5条-倒序
			List<StockAvgBase> clist5 = avgService.queryListByCodeForModelWithLastN(code, date,
					EsQueryPageUtil.queryPage5, true, false);
			// 是否上升趋势
			if (clist5 != null && clist5.size() >= 5) {
				cbm.setShooting51(1);
				cbm.setShooting52(0);
				cbm.setShooting53(1);

				double unp5share = 0;// 总流通(万股)
				if (s.getFloatShare() > 0) {
					double unP5liutonggf = s.getFloatShare() * 10000;
					if (s.getCircZb() > 0) {// 除去5%以上的占比
						unP5liutonggf = ((100 - s.getCircZb()) * unP5liutonggf) / 100;
					}
					unp5share = unP5liutonggf;
				}

				for (int i = 0; i < clist5.size(); i++) {
					// 是否均线排列(连续5天)
					StockAvgBase sa = clist5.get(i);
					if (sa.getAvgPriceIndex30() <= sa.getAvgPriceIndex20()
							&& sa.getAvgPriceIndex20() <= sa.getAvgPriceIndex10()
							&& sa.getAvgPriceIndex30() < sa.getAvgPriceIndex5()) {
					} else {
						cbm.setShooting51(0);
					}

					// 一阳穿N线
					if (sa.getUpdown() > 0 && sa.getClosePrice() >= sa.getAvgPriceIndex30()
							&& sa.getClosePrice() >= sa.getAvgPriceIndex20()
							&& sa.getClosePrice() >= sa.getAvgPriceIndex10()
							&& sa.getClosePrice() >= sa.getAvgPriceIndex5()) {
						cbm.setShooting52(1);
					}

					// 交易活跃
					if (unp5share > 0) {
						double t = sa.getVolume() * 100 / 10000;// 交易(万股)=手x100/10000;
						if (CurrencyUitl.roundHalfUp(t / unp5share) >= 3.5) {// 实际换手3.5%以上

						} else {
							cbm.setShooting53(0);
						}
					} else {
						cbm.setShooting53(0);
					}
				}
			} else {
				cbm.setShooting51(0);
				cbm.setShooting52(0);
				cbm.setShooting53(0);
			}
		} catch (Exception e) {
			log.info("code={}, date={} 计算出错.", code, date);
		}
	}

	// 上影线(上涨情况下：收盘>昨收+(最高-昨收)/2)
	public static boolean isShangYingXian(TradeHistInfoDaliy t) {
		if (t.getTodayChange() > 0) {
			double up = t.getHigh() - t.getYesterdayPrice();// 今天涨了多少
			double half = up / 2;// 中间值
			double t1 = CurrencyUitl.roundHalfUp(half) + t.getYesterdayPrice();
//			System.err.println(t1);
			double mid = CurrencyUitl.multiplyDecimal(t1, 1.01).doubleValue();// 加权1%,1个点,如果涨停回落6%算上影线
//			System.err.println(mid);
//			System.err.println(CurrencyUitl.cutProfit(t.getYesterdayPrice(), t1) + "%");
//			System.err.println(CurrencyUitl.cutProfit(t.getYesterdayPrice(), mid) + "%");
//			System.err.println(CurrencyUitl.cutProfit(t.getYesterdayPrice(), t.getHigh()) + "%");
//			System.err.println(CurrencyUitl.cutProfit(t.getYesterdayPrice(), t.getClosed()) + "%");
			if (mid >= t.getClosed()) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		TradeHistInfoDaliy t = new TradeHistInfoDaliy();
		t.setTodayChange(1);
		t.setYesterdayPrice(7.11);
		t.setHigh(7.81);
		t.setClosed(7.48);
		System.err.println(isShangYingXian(t));
	}

}
