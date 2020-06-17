package com.stable.service.realtime;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.stable.service.TickDataService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TickData;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.up.strategy.ModelV1;

public class RealtimeDetailsAnalyzer implements Runnable {
	private long ONE_MIN = 3 * 60 * 1000;// 1MIN
	private DaliyBasicInfo ytdBasic;
	private StockAvg ytdAvg;
	private TickDataService tickDataService;

	private boolean isRunning = true;

	public void stop() {
		isRunning = false;
	}

	public RealtimeDetailsAnalyzer(ModelV1 modelV1, DaliyBasicInfo ytdBasic, StockAvg ytdAvg,
			TickDataService tickDataService) {
		this.tickDataService = tickDataService;
		this.ytdAvg = ytdAvg;
		this.ytdBasic = ytdBasic;
	}

	public void run() {
		if (ytdAvg == null || ytdBasic == null) {
			WxPushUtil.pushSystem1(
					"实时:数据不全，终止监控。（ytdAvg == null？{" + (ytdAvg == null) + "}）,（ytdBasic == null？" + (ytdBasic == null));
			return;
		}
		double yesterdayPrice = ytdBasic.getYesterdayPrice();
		String code = ytdBasic.getCode();
		// 监控价
		double chkPrice = Arrays.asList(ytdAvg.getAvgPriceIndex3(), ytdAvg.getAvgPriceIndex5(),
				ytdAvg.getAvgPriceIndex10(), ytdAvg.getAvgPriceIndex20(), ytdAvg.getAvgPriceIndex30()).stream()
				.max(Double::compare).get();
		double p3 = CurrencyUitl.topPrice3p(yesterdayPrice);
		if (p3 > chkPrice) {
			chkPrice = p3;
		}

		// 量控量
//		long ytdvol = ytdBasic.getVol();

		while (isRunning) {
			try {
				List<TickData> allTickData = EastmoneySpider.getReallyTick(code);

				if (allTickData != null) {
					double high = allTickData.stream().max(Comparator.comparingDouble(TickData::getPrice)).get()
							.getPrice();
					if (high >= chkPrice) {// 一阳穿N，并涨幅3%以上
						// 需要看量，开高低走，上影线情况 //TODO
						TickDataBuySellInfo d = tickDataService.sumTickData2(code, 0, yesterdayPrice,
								ytdBasic.getCirc_mv(), allTickData, false);
						WxPushUtil.pushSystem1("请关注:" + code + ",市场行为:"
								+ (d.getBuyTimes() > d.getSellTimes() ? "买入" : "卖出") + ",程序单:"
								+ (d.getProgramRate() > 0) + ",买入额:" + CurrencyUitl.covertToString(d.getBuyTotalAmt())
								+ ",卖出额:" + CurrencyUitl.covertToString(d.getSellTotalAmt()) + ",总交易额:"
								+ CurrencyUitl.covertToString(d.getTotalAmt()) + ",请关注量，上影线，高开低走等");
						isRunning = false;
					}
				}

				Thread.sleep(ONE_MIN);// 1分钟//TODO
			} catch (Exception e) {
				e.printStackTrace();
				WxPushUtil.pushSystem1(code + " 监听异常！");
			}
		}
	}

}
