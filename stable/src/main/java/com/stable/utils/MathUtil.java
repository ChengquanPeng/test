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

	public static void main(String[] args) {
		System.err.println(getRandom4bit());
		System.err.println(getRandom4bit());
		System.err.println(getRandom4bit());
	}
}
