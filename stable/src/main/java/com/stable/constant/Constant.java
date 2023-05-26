package com.stable.constant;

import java.nio.charset.Charset;

import com.alibaba.fastjson.parser.ParserConfig;

public class Constant {
	static {
		// fast json 反序列化白名单
		ParserConfig.getGlobalInstance().addAccept("com.stable.vo");
	}
	public static final String CODE_ON_STATUS = "L";
	public static final String CODE_STATUS_W = "W";
	public static final String SESSION_USER = "USER";
	public static final String EMPTY_STRING = "";
	public static final String EMPTY_STRING2 = "''";
	public static final String UTF_8 = "UTF-8";
	public static final Charset DEFAULT_CHARSET = Charset.forName(UTF_8);
	public static final String NULL = "null";
	public static final String FALSE = "false";
	public static final String DOU_HAO = ",";
	public static final String FEN_HAO = ";";
	public static final String NUM_ER = "2";
	public static final String SYMBOL_ = "-";
	public static final String HTML_LINE = "<br/>";
	public static final String AUTO_MONITOR = "Auto-Monitor:";

	public static final String TUI_SHI = "退";

	public static final int END_DATE = 20140101;// 6年

	public static final double WAN_10 = 10 * 10000;// 10万
	public static final double WAN_5 = 5 * 10000;// 5万

	public static final long MY_ID = 10000;// 我的ID

	public static final String LOGINED_URL_USERS = "users";
	public static final String LOGINED_URL_ADMIN = "admin";

	public static final double YI_200 = 200;// 200亿
	// 大宗
	public final static double DZ_RATE = 4.5;// 大宗比例
	public final static double DZ_WARNING_LINE = 3000.0;// 大宗金额：3千万
}
