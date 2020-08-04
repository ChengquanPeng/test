package com.stable.service.realtime;

import com.stable.utils.CurrencyUitl;

import lombok.Data;

@Data
public class RealtimeMsg {

	private static final String BLANK = " ";
	private String code;
	private String codeName;
	private int baseScore;
	private long totalVol;
	private long chkVol1;
	private long chkVol2;
	private double chkPrice = 0.0;
	private double nowPrice = 0.0;
	private boolean isBuyTimes;
	private String firstTimeWarning;
	private long buyTotalAmt;
	private long sellTotalAmt;
	private long totalAmt;

	// String msg = "关注:" + code + " " + codeName + ",市场行为:" + (buytime ? "买入" :
	// "卖出") + ",主力行为:"
	// + (pg ? "Yes" : "No") + ",买入额:" +
	// CurrencyUitl.covertToString(d.getBuyTotalAmt())
	// + ",卖出额:" + CurrencyUitl.covertToString(d.getSellTotalAmt()) + ",总交易额:"
	// + CurrencyUitl.covertToString(d.getTotalAmt()) + ",第一次提醒时间:" +
	// firstTimeWarning
	// + ",提醒次数:" + warningCnt + ",chkPrice:" + chkPrice + ",当前价格:" + nowPrice;

	public String toMessage() {
		StringBuffer sb = new StringBuffer();
		sb.append("关注:").append(code).append(BLANK).append(codeName).append(BLANK)//
				.append(",基本评分:").append(baseScore).append(BLANK)//
				.append(",市场行为:").append((isBuyTimes ? "买入" : "卖出")).append(BLANK)//
				.append(",买入额:").append(CurrencyUitl.covertToString(buyTotalAmt)).append(BLANK)//
				.append(",卖出额:").append(CurrencyUitl.covertToString(sellTotalAmt)).append(BLANK)//
				.append(",总交易额:").append(CurrencyUitl.covertToString(totalAmt)).append(BLANK)//
				.append(",chkPrice:").append(chkPrice).append(BLANK)//
				.append(",当前价格:").append(nowPrice).append(BLANK)//
				.append(",第一次提醒时间:").append(firstTimeWarning).append(BLANK);//
		return sb.toString();
	}
}
