package com.stable.spider.tick;

import lombok.Data;

@Data
public class Tick {

	// 0/09:25:03/5.40/0.00/55340/29883600/S

	private int id;
	private String time;
	private double price;
	private double change;// 涨跌额
	private long vol;// 手
	private double amt;// 额
	private int bs;// 买卖：buy=1,sold=0

	public Tick(String line) {
		String[] fs = line.split("/");
		id = Integer.valueOf(fs[0]);
		time = fs[1];
		price = Double.valueOf(fs[2]);
		change = Double.valueOf(fs[3]);
		vol = Long.valueOf(fs[4]);
		amt = Double.valueOf(fs[5]);
		if ("S".equals(fs[6])) {
			bs = 1;
		}
	}

	public static void main(String[] args) {
		System.err.println(new Tick("0/09:25:03/5.40/0.00/55340/29883600/S"));
	}
}
