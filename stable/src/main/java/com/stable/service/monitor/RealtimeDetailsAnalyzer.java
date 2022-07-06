package com.stable.service.monitor;

import java.util.Date;
import java.util.List;

import com.stable.msg.WxPushUtil;
import com.stable.spider.realtime.RealTime;
import com.stable.spider.realtime.RealtimeCall;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.MonitoringUitl;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsAnalyzer implements Runnable {
	public static final long ONE_MIN = 1 * 60 * 1000;// 5MIN
	private static final long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private static final long TEN_MIN = 10 * 60 * 1000;// 5MIN
	private long WAIT_MIN = FIVE_MIN;
	public String code;
	private String codeName;
	private boolean isRunning = true;
	private String today = DateUtil.getTodayYYYYMMDD();
	public List<RtmVo> cps;
	private boolean chkCodeClosed = false;
	private RtmVo qibao;
	private double yearHigh1;

	private boolean burstPointCheckTopPrew = false;// 突破前1%
	private boolean burstPointCheckTop = false;// 突破
	private boolean burstPointCheckSzx = false;// 十字星

	public void stop() {
		isRunning = false;
	}

	public int init(String code, List<RtmVo> t, String codeName, double yh) {
		this.code = code;
		this.codeName = codeName;
		this.cps = t;
		RealTime srt = RealtimeCall.get(code);
		if (srt.getOpen() == 0.0 && srt.getBuy1() == 0.0 && srt.getSell1() == 0.0) {
			log.info("{}  SINA 今日疑似停牌或者可能没有集合竞价", codeName);
			chkCodeClosed = true;
		}
		this.yearHigh1 = yh;
		return 1;
	}

	public void run() {
		for (RtmVo rv : cps) {
			if (rv.getBizPushService() != null) {
				qibao = rv;
			}
		}
		RealTime srt = null;
		if (chkCodeClosed) {// 重新检测停牌
			try {
				Thread.sleep(TEN_MIN);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			srt = RealtimeCall.get(code);
			if (srt.getOpen() == 0.0) {
				log.info("SINA 今日停牌,{}", codeName);
				for (RtmVo rv : cps) {
					WxPushUtil.pushSystem1(rv.getWxpush(), codeName + "今日停牌:" + rv.getMsg());
				}
				return;
			}
		}
		long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		Date date = DateUtil.parseDate(today + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(date);
		// long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
		// DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		if (qibao != null) {
			WAIT_MIN = ONE_MIN;// 起爆一分钟一次
			if (srt == null) {
				srt = RealtimeCall.get(code);
			}
			double today = CurrencyUitl.topPrice(srt.getYesterday(), false);// 今天涨停价格
			if (today < qibao.getOrig().getShotPointPrice()) {
				double wp4 = CurrencyUitl.roundHalfUp((qibao.getOrig().getShotPointPrice() * 0.94));// 起爆点的-4%
				if (wp4 <= today && today < qibao.getOrig().getShotPointPrice()) {// 涨停价+2%触及起爆点时，就提前4%预警
					qibao.warningYellow = wp4;
				}
			}
		}
		String smsg = null;
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

				RealTime rt = RealtimeCall.get(code);
				for (RtmVo rv : cps) {
					smsg = "";
					// 正常价格监听
					boolean isOk = MonitoringUitl.isOkForRt(rv.getOrig(), rt);
					if (isOk) {
						if (rv.waitSend) {
							String st = MonitoringUitl.okMsg(rv.getOrig(), rt);
							smsg = st + "," + rv.getMsg();
							rv.waitSend = false;
						}
					}
					// 一年新高
					if (rt.getHigh() > yearHigh1 && !rv.highPriceGot && yearHigh1 > 0) {
						if ("".equals(smsg)) {
							smsg = " 一年新高! 备注:" + rv.getMsg();
						} else {
							smsg += "一年新高! " + smsg;
						}
						rv.highPriceGot = true;
					}
					// 起爆点
					if (qibao != null) {
						if (!burstPointCheckTop && qibao.getOrig().getShotPointPrice() > 0) {
							if (rt.getHigh() >= qibao.getOrig().getShotPointPrice()) {
								burstPointCheckTop = qibao.bizPushService.PushS2(codeName + qibao.you + "[7]突破买点:"
										+ qibao.getOrig().getShotPointPrice() + " " + qibao.ex);
							} else if (!burstPointCheckTopPrew && rt.getHigh() >= qibao.warningYellow) {
								burstPointCheckTopPrew = qibao.bizPushService
										.PushS2(codeName + qibao.you + "[7]突破买点:" + qibao.getOrig().getShotPointPrice()
												+ "目前:" + qibao.warningYellow + " " + qibao.ex);
							}
						}
						if (!burstPointCheckSzx && qibao.getOrig().getShotPointPriceSzx() > 0
								&& rt.getHigh() >= qibao.getOrig().getShotPointPriceSzx()) {
							burstPointCheckSzx = qibao.bizPushService.PushS2(codeName + qibao.you + " [10]突破买点:"
									+ qibao.getOrig().getShotPointPriceSzx() + " " + qibao.ex);
						}

//						if (!burstPointCheckLow && qibao.getOrig().getShotPointPriceLow() <= rt.getLow()
//								&& rt.getLow() <= qibao.getOrig().getShotPointPriceLow5()) {
//							burstPointCheckLow = true;
//							qibao.bizPushService
//									.PushS2(codeName + " 接近旗形底部买点[:" + qibao.getOrig().getShotPointPriceLow() + "-"
//											+ qibao.getOrig().getShotPointPriceLow5() + "]");
//						}
					}
					// 发送
					if (!smsg.equals("")) {
						WxPushUtil.pushSystem1(rv.getWxpush(), codeName + " " + smsg);
					}
				}

				Thread.sleep(WAIT_MIN);
			} catch (Exception e) {
				if (!isPushedException) {
					WxPushUtil.pushSystem1(code + " 监听异常！");
					isPushedException = true;
					e.printStackTrace();
				}
				try {
					Thread.sleep(FIVE_MIN);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private boolean isPushedException = false;
}
