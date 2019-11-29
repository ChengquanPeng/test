package com.stable.constant;

import java.nio.charset.Charset;

import com.alibaba.fastjson.parser.ParserConfig;

public class Constant {
	static {
		// fast json 反序列化白名单
		ParserConfig.getGlobalInstance().addAccept("com.stable.vo");
	}
	public static boolean NEED_LOGIN = true;
	public static final String SESSION_USER = "USER";
	public static final String EMPTY_STRING = "";
	public static final String EMPTY_STRING2 = "''";
	public static final String UTF_8 = "UTF-8";
	public static final Charset DEFAULT_CHARSET = Charset.forName(UTF_8);
	public static final String NULL = "null";
	public static final String FALSE = "false";
	public static final String DOU_HAO = ",";
	public static final String NUM_ER = "2";
	public static final String SYMBOL_ = "-";
}
