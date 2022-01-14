package com.stable.service.monitor;

import java.util.Date;

import com.stable.enums.MonitorType;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.MonitoringUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPool;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsAnalyzer implements Runnable {
	private static final long ONE_MIN = 1 * 60 * 1000;// 5MIN
	private static final long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private static final long TEN_MIN = 10 * 60 * 1000;// 5MIN
	private long WAIT_MIN = FIVE_MIN;
	private String code;
	private String codeName;
	private boolean isRunning = true;
	private String today = DateUtil.getTodayYYYYMMDD();
	private RealtimeDetailsResulter resulter;
	private MonitorPool cp;
	private boolean waitSend = true;
	private boolean chkCodeClosed = false;
	private CodeBaseModel2 cbm;
	private boolean highPriceGot = false;

	public void stop() {
		isRunning = false;
	}

	public int init(String code, MonitorPool cp, RealtimeDetailsResulter resulter, String codeName,
			CodeBaseModel2 cbm) {
		this.code = code;
		this.codeName = codeName;
		this.resulter = resulter;
		this.cp = cp;
		if (cp.getDownPrice() <= 0 && cp.getDownTodayChange() <= 0 && cp.getUpPrice() <= 0
				&& cp.getUpTodayChange() <= 0) {
			log.info("{} {} 没有在线价格监听", code, codeName);
			return 0;
		}
		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt.getOpen() == 0.0 && srt.getBuy1() == 0.0 && srt.getSell1() == 0.0) {
			log.info("{} {} SINA 今日疑似停牌或者可能没有集合竞价", code, codeName);
			// WxPushUtil.pushSystem1(code + " " + codeName + "今日疑似停牌或者可能没有集合竞价");
			chkCodeClosed = true;
		}
		this.cbm = cbm;
		return 1;
	}

	public void run() {
		String msg = "";
		if (cbm.getPls() == 1) {
			msg = "人工已确定!";
		}
		msg += MonitorType.getCodeName(cp.getMonitor()) + cp.getRemark() + " " + cp.getMsg();
		if (chkCodeClosed) {
			try {
				Thread.sleep(TEN_MIN);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			SinaRealTime srt = SinaRealtimeUitl.get(code);
			if (srt.getOpen() == 0.0) {
				log.info("{} {} SINA 今日停牌,{}", code, codeName, msg);
				WxPushUtil.pushSystem1(code + " " + codeName + "今日停牌:" + msg);
				return;
			}
		}
		long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		Date date = DateUtil.parseDate(today + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(date);
		long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));

		RealtimeMsg rm = new RealtimeMsg();
		rm.setCode(code);
		rm.setCodeName(codeName);
		rm.setModeName(msg);
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

				SinaRealTime srt = SinaRealtimeUitl.get(code);
				boolean isOk = MonitoringUitl.isOkForRt(cp, srt);
				if (isOk) {
					rm.tiggerMessage();
					resulter.addBuyMessage(code, rm);
					if (waitSend) {
						WxPushUtil.pushSystem1(code + " " + codeName + " " + rm.getModeName());
						waitSend = false;
					}
				}
				if (cp.getYearHigh1() > 0 && srt.getHigh() > cp.getYearHigh1() && !highPriceGot) {
					WxPushUtil.pushSystem1(codeName + "(" + code + ") 一年新高! 备注:" + cp.getRemark());
					highPriceGot = true;
				}
				if (now > d1450) {
					WAIT_MIN = ONE_MIN;
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
		// 监听完成，修改监听信息：
		// 1.监听买入:监听状态
		// 2.已买入-卖出:持有天数等待，是否完成卖出
//		long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
//		if ((now > d1450)) {
//			finTodayMoni();
//		}
	}

	private boolean isPushedException = false;
}
