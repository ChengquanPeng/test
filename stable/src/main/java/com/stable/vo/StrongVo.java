package com.stable.vo;

import lombok.Data;

@Data
public class StrongVo {
	// 1强势:短中长期强势=>次数和差值:3/5/10/20/120/250天

	private int strongTimes3;
	private int strongDef3;// 3天个股涨幅是否大于大盘的涨幅,是否假强势（stock 3 day>base 3 day)
	private int strongTimes5;
	private int strongDef5;// 5天个股涨幅是否大于大盘的涨幅,是否假强势（stock 5 day>base 5 day)
	private int strongTimes10;
	private int strongTimes20;
	private int strongTimes120;
	private int strongTimes250;
}
