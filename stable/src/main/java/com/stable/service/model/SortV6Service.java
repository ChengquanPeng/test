package com.stable.service.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.model.data.AvgService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SortV6Service {
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	/**
	 * 1.15个交易日内有9.5%的涨幅,且涨停日有回调
	 */
	public TradeHistInfoDaliyNofq is15DayTodayPriceOk(String code, int date) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage16, SortOrder.DESC);

		// 变倒序为顺序
		List<TradeHistInfoDaliyNofq> asc = new LinkedList<TradeHistInfoDaliyNofq>();
		for (TradeHistInfoDaliyNofq r : l2) {
			asc.add(0, r);
		}

		// 有大涨的交易日
		boolean hasTopPrice = false;
		TradeHistInfoDaliyNofq topDate = null;
		for (TradeHistInfoDaliyNofq r : asc) {
			if (r.getTodayChangeRate() >= 9.5) {
				topDate = r;
				hasTopPrice = true;
				break;
			}
		}
		boolean priceOK = false;
		if (hasTopPrice) {
			int c = 0;
			// 往后的5个交易日收盘低于涨停价（回调）
			for (TradeHistInfoDaliyNofq r : asc) {
				if (r.getDate() > topDate.getDate() && c <= 5) {
					c++;
//					if (c == 1) {// 涨停第二天：是否直接低开低走
//						if (topDate.getClosed() > r.getHigh()) {
//							log.info("{} 涨停日 :{}，第二日:{} 第二天低开低走", code, topDate.getDate(), r.getDate());
//							return null;
//						}
//					}
					if (r.getClosed() < topDate.getYesterdayPrice()) {
						log.info("{} {} 回调过多", code, date);
						return null;
					}
					if (r.getClosed() < topDate.getClosed()) {
						priceOK = true;
						break;
					}
				}
			}
		} else {
			log.info("{} {} 最近15个工作日无大涨交易", code, date);
		}

		if (priceOK) {
			// 涨停日前2个月44个交易日是否有吸货嫌疑
			List<TradeHistInfoDaliyNofq> l3 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0,
					topDate.getDate(), EsQueryPageUtil.queryPage45, SortOrder.DESC);

			TradeHistInfoDaliyNofq bfTopDate = null;
			double bfTopDatePrice = 0.0;
			for (TradeHistInfoDaliyNofq r : l3) {
				if (r.getDate() != topDate.getDate()) {
					if (r.getHigh() > bfTopDatePrice) {
						bfTopDatePrice = r.getHigh();
						bfTopDate = r;
					}
				}
			}

			// 涨停前的最高交易日差价是否有吸货行为，是否有涨停
			boolean hasBfTop = false;
			if (bfTopDatePrice > topDate.getHigh()) {
				double bfLowPrice = 999999999.99;
				double bfLowPrice2 = 0.0;
				for (TradeHistInfoDaliyNofq r : l3) {
					if (r.getDate() < bfTopDate.getDate()) {// 高点以前的拉升
						if (r.getLow() < bfLowPrice) {
							bfLowPrice = r.getLow();
						}
						if (r.getTodayChangeRate() >= 9.5) {
							hasBfTop = true;
						}
					} else if (r.getDate() > bfTopDate.getDate() && r.getDate() < topDate.getDate()) {// 高点以后的回调
						if (bfLowPrice2 == 0.0 || bfLowPrice2 > r.getLow()) {
							bfLowPrice2 = r.getLow();
						}
					}
				}
				// 有涨停且是否有超过30%的拉升
				double persent = CurrencyUitl.cutProfit(bfLowPrice, bfTopDatePrice);
				double persent2 = CurrencyUitl.cutProfit(bfLowPrice2, bfTopDatePrice);
				// log.info("{} {} {}", bfLowPrice, bfTopDatePrice, persent);
				if (hasBfTop && persent >= 25.0 && persent <= 50.0 && persent2 >= 18.0
						&& topDate.getClosed() < bfTopDatePrice) {
					// 吸货之前的高点，未超过35%的整幅
					List<TradeHistInfoDaliy> l4 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
							l3.get(l3.size() - 1).getDate(), EsQueryPageUtil.queryPage100, SortOrder.DESC);
					double maxPrice = l4.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get()
							.getHigh();
					double minPrice = l4.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get()
							.getLow();

					// log.info("{} {} ", minPrice, maxPrice);

					double persent3 = CurrencyUitl.cutProfit(minPrice, maxPrice);
					if (persent3 <= 50.0 && isTradeOkBefor5ForVol(code, topDate.getDate())) {
						return topDate;
					}
				}
			}
			log.info("{} {} 无明显吸货嫌疑", code, date);
		} else {
			log.info("{} {} 无回调", code, date);
		}
		return null;
	}

	/**
	 * 2.均线
	 */
	public boolean isWhiteHorseForSortV6(TradeHistInfoDaliyNofq topDate) {
		if (topDate != null) {
			String code = topDate.getCode();
			EsQueryPageReq req = EsQueryPageUtil.queryPage30;
			// 最近30条-倒序
			List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLast60(code, topDate.getDate(), req,
					true);
			StockAvgBase sa = clist30.get(0);
			if (sa.getAvgPriceIndex30() >= sa.getAvgPriceIndex60()) {
				return true;
			}
			log.info("{} 均线不满足", code);
		}
		return false;
	}

	/**
	 * 箱体新高
	 */
	public boolean isWhiteHorseForSortV7(String code, int date) {
		List<TradeHistInfoDaliy> l1 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date,
				EsQueryPageUtil.queryPage5, SortOrder.DESC);
		TradeHistInfoDaliy maxDate = l1.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed)).get();

		List<TradeHistInfoDaliy> l2 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
				l1.get(l1.size() - 1).getDate(), EsQueryPageUtil.queryPage120, SortOrder.DESC);// 用5个交易日的最后一个

		List<TradeHistInfoDaliy> l3 = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy r : l2) {
			l3.add(r);
		}
		l3.remove(0);// 移除当天
		TradeHistInfoDaliy maxDate2 = l3.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getHigh)).get();
		// 是否半年收盘新高
		if (maxDate.getDate() != maxDate2.getDate() && maxDate.getClosed() > maxDate2.getHigh()) {
			double maxPrice = maxDate2.getHigh();
			TradeHistInfoDaliy td = l3.stream().min(Comparator.comparingDouble(TradeHistInfoDaliy::getLow)).get();
			double minPrice = td.getLow();

			double persent3 = CurrencyUitl.cutProfit(minPrice, maxPrice);
			log.info("{} {},min:{}->{} max:{}->{} 半年整幅{}%", code, date, td.getDate(), td.getLow(), maxDate2.getDate(),
					maxDate2.getHigh(), persent3);
			if (persent3 <= 40.0) {
				return true;
			}
		}
		log.info("{} {} 最近5个交易日收盘不是半年新高", code, date);
		return false;
	}

	public static final Semaphore sempAll = new Semaphore(1);

	EsQueryPageReq req00 = EsQueryPageUtil.queryPage6;

	/**
	 * 5.前面交易-交易量
	 */
	public boolean isTradeOkBefor5ForVol(String code, int date) {
		EsQueryPageReq req = EsQueryPageUtil.queryPage6;
		List<TradeHistInfoDaliyNofq> dailyList0 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				req, SortOrder.DESC);// 返回的list是不可修改对象
		List<TradeHistInfoDaliyNofq> nlist = new ArrayList<TradeHistInfoDaliyNofq>();
		for (TradeHistInfoDaliyNofq td : dailyList0) {
			nlist.add(td);
		}

		// 实时不需要调用这个
		TradeHistInfoDaliyNofq today = nlist.get(0);
		nlist.remove(0);// 移除当天
		double vol = today.getVolume();
		double total = 0.0;
		int endIndx = nlist.size();// 涨幅较大，计算5天
		for (int i = 0; i < endIndx; i++) {
			total += nlist.get(i).getVolume();
		}
		// 均值*基数
		double chkVol = Double.valueOf(total / endIndx * 1.8);
		if (vol > chkVol) {
			log.info("当日明显放量近1倍 {},{}", code, date);
			// 明显放量的不能买
			return false;
		}
		return true;

	}
}
