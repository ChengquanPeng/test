package com.stable.utils;

import java.util.Random;

public class MathUtil {

	public static final int getRandomSecBetween5And15() {
		Random r = new Random();
		int number = r.nextInt(10);
		return (number + 5);
	}

	public static final int getRandomSecBetween1And5() {
		Random r = new Random();
		int number = r.nextInt(5);
		return (number + 1);
	}

	public static final int getRandomSecBetween1And30() {
		Random r = new Random();
		int number = r.nextInt(30);
		return (number + 1);
	}

	public static final int getRandomSecBetween15And30() {
		Random r = new Random();
		int number = r.nextInt(30);
		return (number + 15);
	}

	public static final int getRandom4bit() {
		Random r = new Random();
		int s = r.nextInt(1000);
		int e = r.nextInt(1000);
		return (s + (e + 15));
	}

	public static final int getRandomSecBetween0And9() {
		Random r = new Random();
		int number = r.nextInt(10);
		return (number);
	}

	public static final String getRandomLengthStr(int length) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			sb.append(getRandomSecBetween0And9());
		}
		return sb.toString();
	}

	public static final String getRandomLengthStr4() {
		return getRandomLengthStr(4);
	}

	public static void main(String[] args) {
		for (int i = 0; i < 1000; i++) {
			System.err.println(getRandomLengthStr4());
		}
		System.out.println("=");
//		System.err.println(getRandom4bit());
//		System.err.println(getRandom4bit());
//		System.err.println(getRandom4bit());
	}
}
