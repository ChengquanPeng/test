package com.stable.spider.realtime;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.HttpUtil;

public class Realtime163 implements RealtimeProxy {

	private static final String START_PRIX = "_ntes_quote_callback(";
	private static final String URL_FORMAT = "https://api.money.126.net/data/feed/%s,money.api";

	private static String getUrl(String code) {
		if (code.startsWith("6")) {
			return "0" + code;
		} else {
			return "1" + code;
		}
	}

	public RealTime get(String code) {

		try {
			String key = getUrl(code);
			String result = HttpUtil.doGet2(String.format(URL_FORMAT, key));
			result = result.substring(START_PRIX.length(), result.length() - 2);
//			System.err.println(result);
			JSONObject object = JSON.parseObject(result);
			JSONObject t = object.getJSONObject(key);
			RealTime rt = new RealTime();
			rt.setOpen(t.getDoubleValue("open"));
			rt.setYesterday(t.getDoubleValue("yestclose"));
			rt.setNow(t.getDoubleValue("price"));
			rt.setHigh(t.getDoubleValue("high"));
			rt.setLow(t.getDoubleValue("low"));
			rt.setBuy1(t.getDoubleValue("bid1"));
			rt.setSell1(t.getDoubleValue("ask1"));
			rt.setDealNums(t.getLongValue("volume"));// 成交的股票数，由于股票交易以一百股为基本单位，所以在使用时，通常把该值除以一百；
			rt.setSource(2);
			return rt;
		} catch (Exception e) {
			return null;
		}

	}

	public static void main(String[] args) {
		System.err.println(new Realtime163().get("601288"));
	}
}
