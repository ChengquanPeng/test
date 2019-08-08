package com.stable.utils;

import java.util.Random;

public class MathUtil {

	public static final int getRandomSecBetween5And15() {
		Random r = new Random();
		int number = r.nextInt(10);
		return (number + 5);
	}
	
	public static void main(String[] args) {
		System.err.println(getRandomSecBetween5And15());
		System.err.println(getRandomSecBetween5And15());
		System.err.println(getRandomSecBetween5And15());
	}
}
