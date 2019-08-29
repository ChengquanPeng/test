package com.stable.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	public static final String YYYY_MM_DD = "yyyyMMdd";

	public static String getTodayYYYYMMDD() {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		return format.format(new Date());
	}

	public static String getLastDayOfYearYYYYMMDD() {
		int year = getCurYYYY();
		return year + "1231";
	}

	public static int getCurYYYY() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.YEAR);
	}

	public static int getCurJidu() {
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH) + 1;
		if (1 <= month && month <= 3) {
			return 1;
		}
		if (4 <= month && month <= 6) {
			return 2;
		}
		if (7 <= month && month <= 9) {
			return 3;
		}
		if (10 <= month && month <= 12) {
			return 4;
		}
		return 0;
	}

	public static void main(String[] args) {
		System.err.println(getCurYYYY());
	}
}
