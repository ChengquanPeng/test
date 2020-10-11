package com.stable.vo.retrace;

import java.util.HashMap;
import java.util.Map;

import com.stable.utils.CurrencyUitl;

import lombok.ToString;

@ToString
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
			double total = 0;
			for (Integer key : map_act.keySet()) {
				int cnt = map_act.get(key);
				total += cnt;
			}
			for (Integer key : map_act.keySet()) {
				int cnt = map_act.get(key);
				sb.append("@亏损" + key + "%的次数:[" + cnt + "],区间占比:["
						+ CurrencyUitl.roundHalfUpWhithPercent((Double.valueOf(cnt) / total)) + "%];");
			}
		}
		return sb.toString();
	}

	public String getStatLossLowPrice() {
		StringBuffer sb = new StringBuffer("");
		if (map_MinLowPrice.size() > 0) {
			double total = 0;
			for (Integer key : map_MinLowPrice.keySet()) {
				int cnt = map_MinLowPrice.get(key);
				total += cnt;
			}
			for (Integer key : map_MinLowPrice.keySet()) {
				int cnt = map_MinLowPrice.get(key);
				sb.append("@亏损" + key + "%的次数:[" + cnt + "],区间占比:["
						+ CurrencyUitl.roundHalfUpWhithPercent((Double.valueOf(cnt) / total)) + "%];");
			}
		}
		return sb.toString();
	}

	public String getStatLossLowClosedPrice() {
		StringBuffer sb = new StringBuffer("");
		if (map_LowClosedPrice.size() > 0) {
			double total = 0;
			for (Integer key : map_LowClosedPrice.keySet()) {
				int cnt = map_LowClosedPrice.get(key);
				total += cnt;
			}
			for (Integer key : map_LowClosedPrice.keySet()) {
				int cnt = map_LowClosedPrice.get(key);
				sb.append("@亏损" + key + "%的次数:[" + cnt + "],区间占比:["
						+ CurrencyUitl.roundHalfUpWhithPercent((Double.valueOf(cnt) / total)) + "%];");
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

	public double getTotalProfit() {
		return totalProfit;
	}

	public void setTotalProfit(double totalProfit) {
		this.totalProfit = totalProfit;
	}

	public double getTotalLoss() {
		return totalLoss;
	}

	public void setTotalLoss(double totalLoss) {
		this.totalLoss = totalLoss;
	}

	public int getCnt_up() {
		return cnt_up;
	}

	public void setCnt_up(int cnt_up) {
		this.cnt_up = cnt_up;
	}

	public int getCnt_down() {
		return cnt_down;
	}

	public void setCnt_down(int cnt_down) {
		this.cnt_down = cnt_down;
	}

	public int getAct_cnt_up() {
		return act_cnt_up;
	}

	public void setAct_cnt_up(int act_cnt_up) {
		this.act_cnt_up = act_cnt_up;
	}

	public double getAct_totalProfit() {
		return act_totalProfit;
	}

	public void setAct_totalProfit(double act_totalProfit) {
		this.act_totalProfit = act_totalProfit;
	}

	public Map<Integer, Integer> getMap_act() {
		return map_act;
	}

	public void setMap_act(Map<Integer, Integer> map_act) {
		this.map_act = map_act;
	}

	public Map<Integer, Integer> getMap_LowClosedPrice() {
		return map_LowClosedPrice;
	}

	public void setMap_LowClosedPrice(Map<Integer, Integer> map_LowClosedPrice) {
		this.map_LowClosedPrice = map_LowClosedPrice;
	}

	public Map<Integer, Integer> getMap_MinLowPrice() {
		return map_MinLowPrice;
	}

	public void setMap_MinLowPrice(Map<Integer, Integer> map_MinLowPrice) {
		this.map_MinLowPrice = map_MinLowPrice;
	}
}