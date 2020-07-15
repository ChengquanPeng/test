package com.stable.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	public static final String YYYY_MM_DD_HH_MM_SS_NO_SPIT = "yyyyMMddHHmmss";
	public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	public static final String YYYY_MM_DD = "yyyyMMdd";
	public static final String YYYY_MM_DD2 = "yyyy-MM-dd";
	public static final String YYYY_MM_DD3_HHMMSS = "yyyyMMdd HH:mm:ss";

	public static String convertDate(String yyyyMMdd) {
		try {
			SimpleDateFormat format1 = new SimpleDateFormat(YYYY_MM_DD);
			SimpleDateFormat format2 = new SimpleDateFormat(YYYY_MM_DD2);
			return format2.format(format1.parse(yyyyMMdd));
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("ParseException:yyyyMMdd:" + yyyyMMdd);
		}
	}

	public static String formatYYYYMMDD(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		return format.format(date);
	}

	public static Date parseDate(int yyyyMMdd) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		try {
			return format.parse(String.valueOf(yyyyMMdd));
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("ParseException:yyyyMMdd:" + yyyyMMdd);
		}
	}

	public static Date parseDate(String yyyyMMdd) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
		try {
			return format.parse(yyyyMMdd);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("ParseException:yyyyMMdd:" + yyyyMMdd);
		}
	}

	public static Date parseDate(String str, String formatter) {
		SimpleDateFormat format = new SimpleDateFormat(formatter);
		try {
			return format.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("ParseException:formatter:" + str + "," + formatter);
		}
	}

	public static String getTodayYYYYMMDDHHMMSS() {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
		return format.format(new Date());
	}

	public static Date parseTodayYYYYMMDDHHMMSS(String time) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD3_HHMMSS);
		try {
			return format.parse(time);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("ParseException:formatter:" + time + "," + YYYY_MM_DD3_HHMMSS);
		}
	}

	public static String getTodayYYYYMMDDHHMMSS_NOspit() {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		return format.format(new Date());
	}

	public static Long getTodayYYYYMMDDHHMMSS_NOspit(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_NO_SPIT);
		return Long.valueOf(format.format(date));
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

	public static Date addDate(String date, int days) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD);
			Calendar cal = Calendar.getInstance();
			cal.setTime(format.parse(date));
			cal.add(Calendar.DATE, days);
			return cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
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

	public static int getPreYear(int yyyyMMdd) {
//		Date date = new Date();//获取当前时间    
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(parseDate(yyyyMMdd));
		calendar.add(Calendar.YEAR, -1);// 当前时间减去一年，即一年前的时间    
		Date py = calendar.getTime();// 获取一年前的时间，或者一个月前的时间  
		return Integer.valueOf(formatYYYYMMDD(py));
	}

	public static void main(String[] args) {
		System.err.println(getTodayBefor7DayYYYYMMDD());
	}
}
