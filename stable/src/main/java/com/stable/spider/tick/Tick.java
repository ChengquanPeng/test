package com.stable.spider.tick;

import lombok.Data;

@Data
public class Tick {

	private String id;
	private String time;
	private double price;
	private double change;// 涨跌额
	private long vol;// 手
	private double amt;// 额
	private int bs;// 买卖：buy=1,sold=0

	private int fen;

	public String getStandardLine() {
		return id + "," + time + "," + price + "," + change + "," + vol + "," + amt + "," + bs + "," + fen;
	}

	// 腾讯时间
	public void setTencentTime(String time) {
		this.time = time;
		String[] s = time.split(":");
		fen = Integer.valueOf(s[0] + s[1]);
	}

	public void setValByStdLine(String line) {
		// 4445,15:00:01,5.47,0.0,21953,1.2008291E7,1,
		String[] fs = line.split(",");
		this.setId(fs[0]);
		this.setTime(fs[1]);
		this.setPrice(Double.valueOf(fs[2]));
		this.setChange(Double.valueOf(fs[3]));
		this.setVol(Long.valueOf(fs[4]));
		this.setAmt(Double.valueOf(fs[5]));
		if ("S".equals(fs[6])) {
			this.setBs(1);
		}
		this.setFen(Integer.valueOf(fs[7]));
	}

	public static void main(String[] args) {
		Tick t = new Tick();
		t.setValByStdLine("4445,15:00:01,5.47,0.0,21953,1.2008291E7,1,");
		System.err.println(t);
		System.err.println(t.getAmt() / 10000);
	}
}
