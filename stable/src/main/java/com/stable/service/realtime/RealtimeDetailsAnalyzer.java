package com.stable.service.realtime;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.TickDataService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TickData;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsAnalyzer implements Runnable {
	private static final EsQueryPageReq queryPage = new EsQueryPageReq(3);
	private long ONE_MIN = 3 * 60 * 1000;// 1MIN
	private StockAvg ytdAvg;
	private TickDataService tickDataService;
	private DaliyBasicHistroyService daliyBasicHistroyService;
	private String code;
	private int date;
	private boolean isRunning = true;
	private VolAvg d1 = null;
	private VolAvg d2 = null;
	private VolAvg d3 = null;

	public void stop() {
		isRunning = false;
	}

	public RealtimeDetailsAnalyzer(ModelV1 modelV1, DaliyBasicHistroyService daliyBasicHistroyService, StockAvg ytdAvg,
			TickDataService tickDataService) {
		this.tickDataService = tickDataService;
		this.ytdAvg = ytdAvg;
		code = modelV1.getCode();
		date = modelV1.getDate();
	}

	@Getter
	class VolAvg {
		private long v15;// 15分钟均量
		private long v30;// 30分钟均量
		private long v60;// 60分钟均量

		public VolAvg(long historyVol) {
			v15 = historyVol / (4 * 4);
			v30 = historyVol / (4 * 2);
			v30 = historyVol / (4 * 1);
		}
	}

	public void run() {
		DaliyBasicInfo ytdBasic = daliyBasicHistroyService.queryListByCodeForRealtime(code, date);
		if (ytdAvg == null || ytdBasic == null) {
			WxPushUtil.pushSystem1(
					"实时:数据不全，终止监控。ytdAvg==null？" + (ytdAvg == null) + "},ytdBasic==null？" + (ytdBasic == null));
			return;
		}
		List<DaliyBasicInfo> list3 = daliyBasicHistroyService.queryListByCodeForModel(code, date, queryPage)
				.getContent();

		d1 = new VolAvg(ytdBasic.getVol());
		d2 = new VolAvg(list3.get(1).getVol());
		d3 = new VolAvg(list3.get(2).getVol());

		double yesterdayPrice = ytdBasic.getYesterdayPrice();

		// 监控价-1一阳N线价
		double p1 = Arrays.asList(ytdAvg.getAvgPriceIndex3(), ytdAvg.getAvgPriceIndex5(), ytdAvg.getAvgPriceIndex10(),
				ytdAvg.getAvgPriceIndex20(), ytdAvg.getAvgPriceIndex30()).stream().max(Double::compare).get();
		// 监控价-2最少3%
		double p2 = CurrencyUitl.topPrice3p(yesterdayPrice);
		// 监控价-3大于前三天最高价
		double p3 = Arrays.asList(ytdBasic.getHigh(), list3.get(1).getHigh(), list3.get(2).getHigh()).stream()
				.max(Double::compare).get();
		// 最终监控价
		double chkPrice = Arrays.asList(p1, p2, p3).stream().max(Double::compare).get();
		// 量控量
//		long ytdvol = ytdBasic.getVol();
		String today = DateUtil.getTodayYYYYMMDD();

		long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		Date date = DateUtil.parseDate(today + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(date);

		while (isRunning) {
			try {

				long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
				if (d1130 <= now && now <= d1300) {
					long from3 = new Date().getTime();
					int millis = (int) ((date.getTime() - from3));
					log.info("{},中场休息。", code);
					if (millis > 0) {
						Thread.sleep(millis);
					}
				}

				List<TickData> allTickData = EastmoneySpider.getReallyTick(code);
				log.info("{} allTickData size:{}", code, allTickData.size());
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

	public boolean checkVolOk(List<TickData> allTickData) {
		TickData td = allTickData.get(allTickData.size() - 1);

		if (td.getInttime() > 144500) {

		}
		return false;
	}

}
