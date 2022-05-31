package com.stable.service.model.data;

import java.util.List;

import com.stable.constant.EsQueryPageUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.StockAvgBase;

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
	public static void avgLineUp(CodeBaseModel2 cbm, AvgService avgService, String code, int date) {
		try {
			// 最近5条-倒序
			List<StockAvgBase> clist5 = avgService.queryListByCodeForModelWithLastN(code, date,
					EsQueryPageUtil.queryPage5, true, false);
			// 是否上升趋势
			if (clist5 != null && clist5.size() >= 5) {
				cbm.setShooting51(1);
				cbm.setShooting52(0);

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
				}
			} else {
				cbm.setShooting51(0);
				cbm.setShooting52(0);
			}
		} catch (Exception e) {
			log.info("code={}, date={} 计算出错.", code, date);
		}
	}
}
