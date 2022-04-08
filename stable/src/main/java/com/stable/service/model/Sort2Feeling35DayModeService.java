package com.stable.service.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

@Service
public class Sort2Feeling35DayModeService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private ModelWebService modelWebService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

	private final double mkvcheckLine = 100.0;// 市值
	private double chkrateline = -7;// 3-5天跌幅
	private final double upchkLine = 65.0;// 一年涨幅超过

	public boolean sort2ModeChk(String code, double mkv, int date) {
		try {
			if (mkv >= mkvcheckLine) {// 100亿
				if (isPriceVolOk(code, date) && isKline(code, date)) {

					return true;//
				}
			}
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, code, date, mkv);
		}
		return false;
	}

	/**
	 * 一年涨了65%?
	 */
	private boolean isKline(String code, int date) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage250, SortOrder.DESC);

		TradeHistInfoDaliyNofq topDate = l2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getHigh))
				.get();
		double maxPrice = topDate.getHigh();

		TradeHistInfoDaliyNofq lowDate = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getLow))
				.get();
		double minPrice = lowDate.getLow();
		// d1.getDate() < d2.getDate():是上涨趋势。
		if (lowDate.getDate() < topDate.getDate() && CurrencyUitl.cutProfit(minPrice, maxPrice) >= upchkLine) {
			return true;
		}
		return false;
	}

//	@javax.annotation.PostConstruct
	public void test() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.err.println("========= start =========");
				int date = 20220401;
				StringBuffer shootNotice6 = new StringBuffer();
				Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
				int i = 0;
				for (CodeBaseModel2 cbm : histMap.values()) {
					DaliyBasicInfo2 d = daliyBasicHistroyService.queryLastest(cbm.getCode(), 0, 0);
					System.err.println("code:" + cbm.getCode());
					if (d == null) {
						System.err.println("====> null:" + cbm.getCode());
						continue;
					}
					sort2ModeChk(d.getCode(), d.getCircMarketVal(), date);
					if (cbm.getShooting6() == 1) {
						i++;
						shootNotice6.append(cbm.getCode()).append("\n");
					}
					if (i >= 10) {
						break;
					}
				}
				System.err.println("========= done =========");
				System.err.println(shootNotice6.toString());
			}
		}).start();
	}

	// 1.短期大幅下跌且缩量
	private boolean isPriceVolOk(String code, int date) {
		boolean isOk = false;
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage5, SortOrder.DESC);
		TradeHistInfoDaliyNofq today = l2.get(0);
		TradeHistInfoDaliyNofq preday = l2.get(1);

		// 1.放巨量在水上？？

		// 第一种情况：连续两天大幅放量下跌超过-8,量缩减
		if (today.getTodayChangeRate() <= 0.0 && preday.getTodayChangeRate() <= 0.0
				&& (chkrateline > (today.getTodayChangeRate() + preday.getTodayChangeRate()))) {
			if (preday.getVolume() > (today.getVolume() * 2)) {
				return true;
			}
		}
		// 第二种情况：连续3天下跌超过-8,量缩减
		if (today.getTodayChangeRate() <= 0.0 && preday.getTodayChangeRate() <= 0.0
				&& l2.get(2).getTodayChangeRate() <= 0.0 && (chkrateline > (today.getTodayChangeRate()
						+ preday.getTodayChangeRate() + l2.get(2).getTodayChangeRate()))) {
			if (today.getVolume() < preday.getVolume() && preday.getVolume() < l2.get(2).getVolume()) {
				if (l2.get(2).getVolume() > (today.getVolume() * 2)) {
					return true;
				}
			}
		}
		return isOk;
	}

	public static void main(String[] args) {
		double k = 100000;// 10万本金
		for (int i = 0; i < 22; i++) {// 一年250天
			k = k * 1.005;
		}
		System.err.println(k);
	}
}
