package com.stable.service.trace;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.CodePoolService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.model.data.AvgService;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodePool;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SortV5Service {
	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	// 箱体新高（3个月新高，短期有8%的涨幅）
	public void sortv5(int tradeDate) {
		StringBuffer msg = new StringBuffer();
		List<Integer> pa = new ArrayList<Integer>();// 1大牛，2中线，3人工，4短线
		pa.add(1);
		pa.add(3);
		List<CodePool> list = codePoolService.queryForSortV5(pa);
		if (list != null && list.size() > 0) {
			for (CodePool cp : list) {
				String code = cp.getCode();
				log.info(cp);
				if (isTodayPriceOk(code, tradeDate) && isWhiteHorseForSortV5(code, tradeDate)) {
					msg.append(code).append(",");
				}
			}
		}
		if (msg.length() > 0) {
			WxPushUtil.pushSystem1("股票池监听启动股票:" + msg.toString());
		}
	}

	/**
	 * 1.3个月新高，短期有9.5%的涨幅
	 */
	private boolean isTodayPriceOk(String code, int date) {
		EsQueryPageReq page = EsQueryPageUtil.queryPage60;
		// 3个月新高，22*3=60
		DaliyBasicInfo daliy = daliyBasicHistroyService.getDaliyBasicInfoByDate(code, date);
		List<TradeHistInfoDaliy> listD60 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
				daliy.getTrade_date(), page, SortOrder.DESC);
		if (listD60 == null || listD60.size() < page.getPageSize()) {
			log.info("{} 未获取到3个月的前复权交易记录", code);
			return false;
		}
		boolean isTopOK = true;
		for (TradeHistInfoDaliy td : listD60) {
			if (td.getHigh() > daliy.getHigh()) {
				isTopOK = false;
			}
		}
		if (isTopOK) {
			List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0,
					daliy.getTrade_date(), EsQueryPageUtil.queryPage5, SortOrder.DESC);
			for (TradeHistInfoDaliyNofq r : l2) {
				if (r.getTodayChangeRate() >= 9.5) {
					return true;
				}
			}
			log.info("{} 最近5个工作日无大涨交易", code);
		} else {
			log.info("{} 非3个月新高", code);
		}
		return false;
	}

	/**
	 * 2.均线
	 */
	private boolean isWhiteHorseForSortV5(String code, int date) {
		EsQueryPageReq req = EsQueryPageUtil.queryPage30;
		// 最近30条-倒序
		List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLast60(code, date, req, true);
		StockAvgBase sa = clist30.get(0);
		if (sa.getAvgPriceIndex30() >= sa.getAvgPriceIndex60() && sa.getAvgPriceIndex20() >= sa.getAvgPriceIndex30()
				&& sa.getAvgPriceIndex20() >= sa.getAvgPriceIndex5()) {
			return true;
		}
		log.info("{} 均线不满足", code);
		return false;
	}

}
