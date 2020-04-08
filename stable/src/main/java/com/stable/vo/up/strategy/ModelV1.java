package com.stable.vo.up.strategy;

import lombok.Data;

@Data
public class ModelV1 {

	private String code;
	private int date;
	private double close; 

	// L1
	// 1强势:次数和差值:3/5/10/20/120/250天
	private int strongTimes3;
	private int strongDef3;// 3天个股涨幅是否大于大盘的涨幅,是否假强势（stock 3 day>base 3 day)
	private int strongTimes5;
	private int strongDef5;// 5天个股涨幅是否大于大盘的涨幅,是否假强势（stock 5 day>base 5 day)
	private int strongTimes10;
	private int strongTimes20;
	private int strongTimes120;
	private int strongTimes250;
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
	private double avgIndex3;
	private double avgIndex5;
	private double avgIndex10;
	private double avgIndex20;
	private double avgIndex30;
	private double avgIndex120;
	private double avgIndex250;
	// 价格底部指数
	private int priceIndex;
	// L2
	// 板块活跃度
	// 建仓行为
	// 回购/分红/业绩
}
