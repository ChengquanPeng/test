package com.stable.utils.talib;

import com.stable.utils.CurrencyUitl;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class SimpleMovingAverageExample {

	/**
	 * The total number of periods to generate data for.
	 */
	public static final int TOTAL_PERIODS = 100;

	/**
	 * The number of periods to average together.
	 */
	public static final int PERIODS_AVERAGE = 30;

	public static void main(String[] args) {
		double[] closePrice = new double[TOTAL_PERIODS];
		double[] out = new double[TOTAL_PERIODS];
		MInteger begin = new MInteger();
		MInteger length = new MInteger();

		for (int i = 0; i < closePrice.length; i++) {
			closePrice[i] = (double) i;
//			System.err.println("i=" + i);
		}

		Core c = new Core();
		RetCode retCode = c.sma(0, closePrice.length - 1, closePrice, PERIODS_AVERAGE, begin, length, out);

		if (retCode == RetCode.Success) {

			System.out.println("Output Start Period: " + begin.value);
			System.out.println("Output End Period: " + (begin.value + length.value - 1));

			for (int i = begin.value; i < begin.value + length.value; i++) {
				StringBuilder line = new StringBuilder();
				line.append("Period #");
				line.append(i);
				line.append(" close=");
				line.append(closePrice[i]);
				line.append(" mov_avg=");
				line.append(out[i - begin.value]);
				System.out.println(line.toString());
			}
			System.out.println("=========");
			System.out.println(begin.value);
			System.out.println(length.value);
			System.out.println("=========");
			for (int i = 0; i < out.length; i++) {
				System.err.println(CurrencyUitl.roundHalfUp(out[i]));
			}
		} else {
			System.out.println("Error");
		}
	}
}