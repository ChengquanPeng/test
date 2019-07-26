package com.stable.constant;

import java.nio.charset.Charset;

import com.alibaba.fastjson.parser.ParserConfig;

public class Constant {
	static {
		//fast json 反序列化白名单
        ParserConfig.getGlobalInstance().addAccept("com.stable.vo");
    }
	public static final String EMPTY_STRING = "";
	public static final String UTF_8 = "UTF-8";
	public static final Charset DEFAULT_CHARSET = Charset.forName(UTF_8);
}
