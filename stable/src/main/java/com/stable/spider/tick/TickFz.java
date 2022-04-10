package com.stable.spider.tick;

import lombok.Data;

@Data
public class TickFz {

	// 1分钟
	public int fen;
	public long vol;// 手
	public double hprice;// 一分钟最高
	public double lprice;// 一分钟最低
	public int sx;// 1:水上,-1:水下
}
