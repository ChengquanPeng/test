package com.stable.utils;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class TaLabUtil {
	public static double[] sma(double[] closePrice, int periods) {
		int TOTAL_PERIODS = closePrice.length;
		double[] out = new double[TOTAL_PERIODS];
		MInteger begin = new MInteger();
		MInteger length = new MInteger();

		Core c = new Core();
		RetCode retCode = c.sma(0, closePrice.length - 1, closePrice, periods, begin, length, out);

		if (retCode == RetCode.Success) {
			return out;
		}
		return null;
	}
}
