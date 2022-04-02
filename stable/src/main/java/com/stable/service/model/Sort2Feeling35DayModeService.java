package com.stable.service.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

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
	private final double upchkLine = 65.0;// 一年涨幅超过

	public void sort2ModeChk(CodeBaseModel2 cbm, double mkv, int date, StringBuffer shootNotice6) {
		try {
			cbm.setShooting6(0);
			if (mkv >= mkvcheckLine) {// 100亿
				String code = cbm.getCode();
				if (isPriceVolOk(code, date)
				// && isKline(code, date, upchkLine)
				) {
					cbm.setShooting6(1);
				}
			}
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, cbm.getCode(), date, mkv);
		}
	}

	/**
	 * 一年涨了65%?
	 */
	private boolean isKline(String code, int date, double checkLine) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage250, SortOrder.DESC);

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
			return true;
		}
		return false;
	}

	@PostConstruct
	private void test() {
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
					sort2ModeChk(cbm, d.getCircMarketVal(), date, shootNotice6);
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

	private double chkrateline = -7;

	// 1.短期是否大幅下跌缩量
	private boolean isPriceVolOk(String code, int date) {
		boolean isOk = false;
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage5, SortOrder.DESC);
		TradeHistInfoDaliyNofq today = l2.get(0);
		TradeHistInfoDaliyNofq preday = l2.get(1);
		// 第一种情况：连续两天大幅放量下跌炒过-8,量缩减
		if (today.getTodayChangeRate() <= 0.0 && preday.getTodayChangeRate() <= 0.0
				&& (chkrateline > (today.getTodayChangeRate() + preday.getTodayChangeRate()))) {
			if (preday.getVolume() > (today.getVolume() * 2)) {
				return true;
			}
		}
		// 第二种情况：连续3天下跌炒过-8,量缩减
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
		for (int i = 0; i < 250; i++) {// 一年250天
			k = k * 1.005;
		}
		System.err.println(k);
	}
}
