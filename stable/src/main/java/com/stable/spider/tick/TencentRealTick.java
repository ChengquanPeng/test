package com.stable.spider.tick;

import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;

public class TencentRealTick {

	private static String url_base = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/dealinfo/getMingxiV2?code=%s&limit=%s&direction=1&_callback=jQuery%s&_=%s";
	private static String start = "jQuery20220409(";
	private static int start_len = start.length();
	public static int limit = 60;// 60条
	public static int tradeDate = DateUtil.getTodayIntYYYYMMDD();// 实时调用 TODO

	// 获取最新交易-腾讯
	public static void fetchRealTradesLast60(String code) {
		List<TickFb> ticks = new LinkedList<TickFb>();
		String url = String.format(url_base, TencentTick.getCode(code), limit, tradeDate, System.currentTimeMillis());
		String s = HttpUtil.doGet2(url);
//		System.err.println(s);
		if (s != null && s.length() > start_len) {
			String jsonstr = s.substring(start_len, s.length() - 1);
//			System.err.println(jsonstr);
			JSONObject json = JSON.parseObject(jsonstr);
			if (json.getIntValue("code") == 0) {
				JSONObject d1 = json.getJSONObject("data");
				JSONArray d2 = d1.getJSONArray("data");
				for (int i = 0; i < d2.size(); i++) {
					ticks.add(TencentTick.getTick((String) d2.get(i)));
				}

			}
		}
	}

	public static void main(String[] args) {
		// 生成
		fetchRealTradesLast60("301058");
		// 读取
//		List<Tick> list = readFromFile(filepath);
//		for (Tick t : list) {
//			System.err.println(t);
//		}
		System.err.println("==done===");
	}
}
