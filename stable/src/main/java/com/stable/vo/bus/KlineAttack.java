package com.stable.vo.bus;

import lombok.Data;

@Data
public class KlineAttack {

	int month;
	double high;
	double low = Integer.MAX_VALUE;
	double range;

	public void addHigh(double price) {
		if (price > high) {
			high = price;
		}
	}

	public void addLow(double price) {
		if (price < low) {
			low = price;
		}
	}
}
