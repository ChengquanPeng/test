package com.stable.service.realtime;

import java.util.Date;

import com.stable.enums.CodeModeType;
import com.stable.es.dao.base.MonitoringDao;
import com.stable.spider.sina.SinaRealTime;
import com.stable.spider.sina.SinaRealtimeUitl;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodePool;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RealtimeDetailsAnalyzer implements Runnable {
	private long ONE_MIN = 1 * 60 * 1000;// 5MIN
	private long FIVE_MIN = 5 * 60 * 1000;// 5MIN
	private long WAIT_MIN = FIVE_MIN;
	private String code;
	private String codeName;
	private boolean isRunning = true;
	private String today = DateUtil.getTodayYYYYMMDD();
	RealtimeDetailsResulter resulter;
	MonitoringDao monitoringDao;
	CodePool cp;
	private boolean waitSend = true;

	public void stop() {
		isRunning = false;
	}

	public int init(String code, CodePool cp, RealtimeDetailsResulter resulter, String codeName) {
		this.code = code;
		this.codeName = codeName;
		this.resulter = resulter;
		this.cp = cp;

		SinaRealTime srt = SinaRealtimeUitl.get(code);
		if (srt.getOpen() == 0.0) {
			log.info("{} {} SINA 今日停牌", code, codeName);
			WxPushUtil.pushSystem1(code + " " + codeName + "今日停牌");
			return 0;
		}
		return 1;
	}

	public void run() {
		long d1130 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "113000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
		Date date = DateUtil.parseDate(today + "130100", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		long d1300 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(date);
		long d1450 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
				DateUtil.parseDate(today + "145000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));

		RealtimeMsg rm = new RealtimeMsg();
		rm.setCode(code);
		rm.setCodeName(codeName);
		rm.setBaseScore(cp.getScore());
		rm.setModeName(CodeModeType.getCodeName(cp.getMonitor()));
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
				double per = CurrencyUitl.cutProfit(srt.getYesterday(), srt.getNow());
				if (per >= 3.5) {
					rm.tiggerMessage();
					resulter.addBuyMessage(code, rm);
					if (waitSend) {
						WxPushUtil.pushSystem1(code + " " + codeName + " 涨幅超过3.5%");
						waitSend = false;
					}
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
