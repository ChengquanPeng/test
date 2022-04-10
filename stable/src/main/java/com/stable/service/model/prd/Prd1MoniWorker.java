package com.stable.service.model.prd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stable.spider.realtime.RealTime;
import com.stable.spider.realtime.RealtimeCall;
import com.stable.spider.tick.TencentTickReal;
import com.stable.spider.tick.TencentTick;
import com.stable.spider.tick.TencentTickHist;
import com.stable.spider.tick.TickFb;
import com.stable.spider.tick.TickFz;
import com.stable.vo.Prd1Monitor;
import com.stable.vo.bus.OnlineTesting;

public class Prd1MoniWorker implements Runnable {

	private String code;
	private Prd1Monitor pm;
	private double yersterdayPrice;
	private Map<Integer, TickFz> map = new HashMap<Integer, TickFz>();
	private OnlineTesting ot;
	private int help = 9;
	public boolean stopToday = false;
	private int stpchk = 0;

	Prd1MoniWorker(Prd1Monitor p) {
		this.pm = p;
		this.code = p.getCode();
		ot = p.getOnlineTesting();
	}

	public void run() {
		if (yersterdayPrice == 0) {
			RealTime srt = RealtimeCall.get(code);
			if (srt.getOpen() == 0.0) {
				stpchk++;
				if (stpchk >= 5) {
					stopToday = true;
				}
				return;
			}
			if (srt.getYesterday() > 0.0) {
				// 未停牌则设置昨收
				yersterdayPrice = srt.getYesterday();
				TencentTickHist.readFromFile(filepath);
			} else {
				return;
			}

		}
		List<TickFb> fbs = TencentTickReal.fetchRealTradesLast60(code);
		Map<Integer, TickFz> t = TencentTick.getTickFzMap(fbs, yersterdayPrice);
		for (Integer key : t.keySet()) {
			map.put(key, t.get(key));
		}
		boolean needBuy = false;
		// 卖出
		if (ot != null) {
			if (help == soldPoint()) {
				needBuy = true;// 需要补仓
			}
		} else {
			needBuy = true;// 纯买入监听
		}

		// 买入
		if (needBuy) {
			buyPoint();
		}
	}

	// 卖点:
	// 9:已套牢，需要补偿，
	private int soldPoint() {

		return help;
	}

	// 是否获取到买点
	private boolean buyPoint() {

		return false;
	}
}
