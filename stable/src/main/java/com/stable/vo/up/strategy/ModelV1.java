package com.stable.vo.up.strategy;

import lombok.Data;

@Data
public class ModelV1 {

	private String code;
	private int date;

	// L1
	// 1强势:次数和差值:3/5/10/20/120/250天
	private int strongTimes3;
	private Long strongDef3;
	private int strongTimes5;
	private Long strongDef5;
	private int strongTimes10;
	private Long strongDef10;
	private int strongTimes20;
	private Long strongDef20;
	private int strongTimes120;
	private Long strongDef120;
	private int strongTimes250;
	private Long strongDef250;
	// 2交易方向:次数和差值:3/5/10/20/120/250天
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
	// 3程序单:次数:3/5/10/20/120/250天
	private int pgmTimes3;
	private int pgmTimes5;
	private int pgmTimes10;
	private int pgmTimes20;
	private int pgmTimes120;
	private int pgmTimes250;
	// 4均线:指数:3/5/10/20/120/250天
	private int avgIndex3;
	private int avgIndex5;
	private int avgIndex10;
	private int avgIndex20;
	private int avgIndex120;
	private int avgIndex250;
	// 价格底部指数
	private int priceIndex;
	// L2
	// 板块活跃度
	// 业绩
	// 建仓行为
}
