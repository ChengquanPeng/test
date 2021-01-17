package com.stable.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeUtil {

	// 中文转Unicode
	public static String gbEncoding(final String gbString) { // gbString = "测试"
		char[] utfBytes = gbString.toCharArray(); // utfBytes = [测, 试]
		String unicodeBytes = "";
		for (int byteIndex = 0; byteIndex < utfBytes.length; byteIndex++) {
			String hexB = Integer.toHexString(utfBytes[byteIndex]); // 转换为16进制整型字符串
			if (hexB.length() <= 2) {
				hexB = "00" + hexB;
			}
			unicodeBytes = unicodeBytes + "\\u" + hexB;
		}
		System.out.println("unicodeBytes is: " + unicodeBytes);
		return unicodeBytes;
	}

	// Unicode转中文
	public static String decodeUnicode(final String dataStr) {
		int start = 0;
		int end = 0;
		final StringBuffer buffer = new StringBuffer();
		while (start > -1) {
			end = dataStr.indexOf("\\u", start + 2);
			String charStr = "";
			if (end == -1) {
				charStr = dataStr.substring(start + 2, dataStr.length());
			} else {
				charStr = dataStr.substring(start + 2, end);
			}
			char letter = (char) Integer.parseInt(charStr, 16); // 16进制parse整形字符串。
			buffer.append(new Character(letter).toString());
			start = end;
		}
		return buffer.toString();
	}

	/**
	 * unicode编码转换为汉字
	 * 
	 * @param unicodeStr 待转化的编码
	 * @return 返回转化后的汉子
	 */
	public static String UnicodeToCN(String unicodeStr) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(unicodeStr);
		char ch;
		while (matcher.find()) {
			// group
			String group = matcher.group(2);
			// ch:'李四'
			ch = (char) Integer.parseInt(group, 16);
			// group1
			String group1 = matcher.group(1);
			unicodeStr = unicodeStr.replace(group1, ch + "");
		}

		return unicodeStr.replace("\\", "").trim();
	}
}
