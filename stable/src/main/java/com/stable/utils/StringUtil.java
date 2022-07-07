package com.stable.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeConcept;

public class StringUtil {
	/**
	 * 字符串每隔指定长度插入指定字符串
	 */

	public static String stringInsertByInterval(String original, String insertString, int interval) {
		if (original == null)
			return "";
		Integer len = original.length();
		if (interval >= len)
			return original;

		String rtnString = original;
		if (original.length() > interval) {
			List<String> strList = new ArrayList<String>();
			Pattern p = Pattern.compile("(.{" + interval + "}|.*)");
			Matcher m = p.matcher(original);
			while (m.find()) {
				strList.add(m.group());
			}
			strList = strList.subList(0, strList.size() - 1);
			rtnString = StringUtils.join(strList, insertString);
		}
		return rtnString;
	}

	public static String subString(String original, int length) {
		if (original.length() <= length) {
			return original;
		}
		return original.substring(0, length);
	}

	public static String getGn(List<CodeConcept> l) {
		StringBuffer sb = new StringBuffer("");
		if (l != null) {
			for (CodeConcept cc : l) {
				sb.append(cc.getConceptName()).append(",");
			}
		}
		return sb.toString();
	}

	public static String getTag(CodeBaseModel2 cbm) {
		String you = "";
		if (cbm.getShooting7() > 0) {
			you = "[优]";
		} else {
			you = "[普]";
		}
		if (cbm.getShooting11() > 0) {
			you += "[大]" + you;
		}
		if (cbm.getFinDbl() > 0 || (cbm.getFinOK() > 0 && cbm.getFinanceInc() > 0)) {
			you += "[绩]" + you;
		}
		return you;
	}
}
