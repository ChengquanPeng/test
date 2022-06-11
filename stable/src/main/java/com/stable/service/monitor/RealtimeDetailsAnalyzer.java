package com.stable.service.monitor;

import java.util.Date;
import java.util.List;

import com.stable.constant.Constant;
import com.stable.spider.realtime.RealTime;
import com.stable.spider.realtime.RealtimeCall;
import com.stable.utils.DateUtil;
import com.stable.utils.MonitoringUitl;
import com.stable.utils.WxPushUtil;

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
	private boolean burstPointCheck = false;// 起爆点
//	private ShotPointCheck shotPointCheck;
	private RtmVo my;
	private double yearHigh1;

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
			if (rv.getOrig().getUserId() == Constant.MY_ID && rv.getOrig().getShotPointPrice() > 0) {
				my = rv;
			}
		}
		if (chkCodeClosed) {// 重新检测停牌
			try {
				Thread.sleep(TEN_MIN);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			RealTime srt = RealtimeCall.get(code);
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
					if (my != null && !burstPointCheck) {
						if (rt.getHigh() >= my.getOrig().getShotPointPrice()) {
							WxPushUtil.pushSystem1(rv.getWxpush(),
									codeName + " 到达起爆买点:" + my.getOrig().getShotPointPrice());
						}
					}
//					if (my != null && now > d1450) {
//						WAIT_MIN = ONE_MIN;
//						if (!burstPointCheck && my.getOrig().getShotPointCheck() == 1) {
//							burstPointCheck = true;
//							ShotPoint sp = shotPointCheck.check(code, 0, rt);
//							if (sp.getResult()) {
//								if ("".equals(smsg)) {
//									smsg += "疑似起爆:" + sp.getMsg() + "！ 备注:" + rv.getMsg();
//
//								} else {
//									smsg = "疑似起爆:" + sp.getMsg() + smsg;
//								}
//							}
//						}
//					}
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
