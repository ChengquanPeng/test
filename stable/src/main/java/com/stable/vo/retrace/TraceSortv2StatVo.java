package com.stable.vo.retrace;

import java.util.HashMap;
import java.util.Map;

import com.stable.utils.CurrencyUitl;

import lombok.Data;

@Data
public class TraceSortv2StatVo {
	// 理论最高总盈亏
	double totalProfit = 0.0;
	double totalLoss = 0.0;
	int cnt_up = 0;
	int cnt_down = 0;
	// 实际
	// 实际
	int act_cnt_up = 0;
	double act_totalProfit = 0.0;

	// 实际被套区间
	Map<Integer, Integer> map_act = new HashMap<Integer, Integer>();
	// 理论最低收盘价被套区间
	Map<Integer, Integer> map_LowClosedPrice = new HashMap<Integer, Integer>();
	// 理论最低价被套区间
	Map<Integer, Integer> map_MinLowPrice = new HashMap<Integer, Integer>();

	public String getStatLossAct() {
		StringBuffer sb = new StringBuffer("");
		if (map_act.size() > 0) {
			int total = 0;
			for (Integer key : map_act.keySet()) {
				int cnt = map_act.get(key);
				total += cnt;
			}
			for (Integer key : map_act.keySet()) {
				int cnt = map_act.get(key);
				sb.append("@亏损" + key + "%的次数:[" + cnt + "],区间占比:[" + CurrencyUitl.roundHalfUp((cnt / total)) + "%];");
			}
		}
		return sb.toString();
	}

	public String getStatLossLowPrice() {
		StringBuffer sb = new StringBuffer("");
		if (map_MinLowPrice.size() > 0) {
			int total = 0;
			for (Integer key : map_MinLowPrice.keySet()) {
				int cnt = map_MinLowPrice.get(key);
				total += cnt;
			}
			for (Integer key : map_MinLowPrice.keySet()) {
				int cnt = map_MinLowPrice.get(key);
				sb.append("@亏损" + key + "%的次数:[" + cnt + "],区间占比:[" + CurrencyUitl.roundHalfUp((cnt / total)) + "%];");
			}
		}
		return sb.toString();
	}

	public String getStatLossLowClosedPrice() {
		StringBuffer sb = new StringBuffer("");
		if (map_LowClosedPrice.size() > 0) {
			int total = 0;
			for (Integer key : map_LowClosedPrice.keySet()) {
				int cnt = map_LowClosedPrice.get(key);
				total += cnt;
			}
			for (Integer key : map_LowClosedPrice.keySet()) {
				int cnt = map_LowClosedPrice.get(key);
				sb.append("@亏损" + key + "%的次数:[" + cnt + "],区间占比:[" + CurrencyUitl.roundHalfUp((cnt / total)) + "%];");
			}
		}
		return sb.toString();
	}

	public void statLossAct(double loss) {
		if (loss < 0) {
			int key = Double.valueOf(Math.floor(loss)).intValue();
			if (key <= -10) {
				key = -10;
			}

			int cnt = 1;
			if (map_act.containsKey(key)) {
				cnt = cnt + map_act.get(key).intValue();
			}
			map_act.put(key, cnt);
		}
	}

	public void statLossLowClosedPrice(double loss) {
		if (loss < 0) {
			int key = Double.valueOf(Math.floor(loss)).intValue();
			if (key <= -10) {
				key = -10;
			}

			int cnt = 1;
			if (map_LowClosedPrice.containsKey(key)) {
				cnt = cnt + map_LowClosedPrice.get(key).intValue();
			}
			map_LowClosedPrice.put(key, cnt);
		}
	}

	public void statLossLowPrice(double loss) {
		if (loss < 0) {
			int key = Double.valueOf(Math.floor(loss)).intValue();
			if (key <= -10) {
				key = -10;
			}

			int cnt = 1;
			if (map_MinLowPrice.containsKey(key)) {
				cnt = cnt + map_MinLowPrice.get(key).intValue();
			}
			map_MinLowPrice.put(key, cnt);
		}
	}

}