package com.stable.vo.bus;

import lombok.Data;

@Data
public class TickData {
	private String time;
	private double price;
//	private double change;
	private long volume;
	private long amount;
	private String type;
	private int detailNum = 1;

	public String getSecV1() {
		return time.split(":")[2];
	}

}
