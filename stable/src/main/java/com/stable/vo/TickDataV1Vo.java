package com.stable.vo;

import lombok.Data;

@Data
public class TickDataV1Vo {
	// 1强势:短中长期买入=>次数和差值:3/5/10/20/120/250天

	private int wayTimes3;
	private Long wayDef3;
	private int wayTimes5;
	private Long wayDef5;
	private int wayTimes10;
	private Long wayDef10;
	private int wayTimes20;
	private Long wayDef20;
	private int wayTimes120;
	private Long wayDef120;
	private int wayTimes250;
	private Long wayDef250;

	private int pgmTimes3;
	private int pgmTimes5;
	private int pgmTimes10;
	private int pgmTimes20;
	private int pgmTimes120;
	private int pgmTimes250;
}
