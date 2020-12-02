package com.stable.enums;

public enum RunLogBizTypeEnum {
	TRADE_CAL(1, "交易日历同步"), 
	STOCK_LIST(2, "股票列表同步"), 
	FINACE_HISTORY(3, "股票财务报告同步"), 
	BUY_BACK(4, "回购同步"),
	TRADE_HISTROY(5, "日线数据同步（前复权/不复权）"), 
	DIVIDEND(6, "分红除权信息同步"),
	DAILY_BASIC(7, "个股每日指标"),
	DIVIDEND_TRADE_HISTROY(8, "前复权日期重置"),
	TICK_DATA(9, "分笔数据"),
	SHARE_FLOAT(10, "解禁"),
	FINACE_FRIST(11, "预告-快报"), 
	CODE_ATTENTION(12, "关注度"), 
	RETRACE_SH_FIVEDAY(9100, "短线-5日反转");

	public int bcode;
	public String btypeName;

	private RunLogBizTypeEnum(int c, String n) {
		this.bcode = c;
		this.btypeName = n;
	}

	public int getBcode() {
		return bcode;
	}

	public void setBcode(int bcode) {
		this.bcode = bcode;
	}

	public String getBtypeName() {
		return btypeName;
	}

	public void setBtypeName(String btypeName) {
		this.btypeName = btypeName;
	}

	public static RunLogBizTypeEnum getRunLogBizTypeEnum(int code) {
		for (RunLogBizTypeEnum rb : RunLogBizTypeEnum.values()) {
			if (rb.getBcode() == code) {
				return rb;
			}
		}
		return null;
	}
}
