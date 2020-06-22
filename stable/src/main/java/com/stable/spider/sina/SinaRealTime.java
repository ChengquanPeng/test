package com.stable.spider.sina;

import lombok.Data;

@Data
public class SinaRealTime {

	private double open;
	private double yesterday;
	private double now;
	private double high;
	private double low;
	private double buy1;
	private double sell1;
	private long dealNums;// 股
	private double dealAmt;// 元
}
