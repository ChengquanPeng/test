package com.stable.service.model.data;

import java.util.List;

import com.stable.constant.EsQueryPageUtil;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class LineAvgPrice {

	public static boolean isWhiteHorseForMidV2(AvgService avgService, String code, int date) {
		try {
			EsQueryPageReq req = EsQueryPageUtil.queryPage30;
			// 最近30条-倒序
			List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLast60(code, date, req, true);
			// 60日是否上升趋势
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
}
