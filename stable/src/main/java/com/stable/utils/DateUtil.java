package com.stable.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	public static final String YYYY_MM_DD = "yyyyMMdd";

	public static Date parseDate(String yyyyMMdd) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		try {
			return format.parse(yyyyMMdd);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("ParseException:yyyyMMdd:" + yyyyMMdd);
		}
	}

	public static String getTodayYYYYMMDDHHMMSS() {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
		return format.format(new Date());
	}

	public static String getTodayBefor7DayYYYYMMDD() {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		return format.format(addDate(new Date(), -7));
	}

	public static Date addDate(Date date, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	public static String getTodayYYYYMMDD() {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		return format.format(new Date());
	}

	public static String getYYYYMMDD(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		return format.format(date);
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
		System.err.println(getTodayBefor7DayYYYYMMDD());
	}
}
