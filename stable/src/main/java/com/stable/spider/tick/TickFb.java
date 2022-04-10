package com.stable.spider.tick;

import lombok.Data;

@Data
public class TickFb {

	private String id;
	private String time;
	private double price;
	private double change;// 涨跌额
	private long vol;// 手
	private double amt;// 额
	private int bs;// 买卖：buy=1,sold=0
	private int fen;
}
