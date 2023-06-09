package com.stable.spider.ths;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.UnicodeUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ThsAnnSpider {

	private static String BASE_URL = "http://basic.10jqka.com.cn/api/stock/getsnlist/%s_%s.json";

	/**
	 * 是否购买资产
	 */
	public static String isBuyAsset(String code, int endDate) {
		if (endDate < 20160101) {
			endDate = 20160101;
		}
		String org = "";
//		String UnicodeToCN = "";
		int i = 1;
		while (true) {// 30页
			String url = String.format(BASE_URL, code, i);
			int trytime = 0;
			boolean fetched = false;
			do {
				try {
//					log.info(url);
					ThreadsUtil.sleepSleep1Seconds();
					org = HttpUtil.doGet2(url);
//					UnicodeToCN = UnicodeUtil.UnicodeToCN(org); 整个json有双引号的情况，所以要下面title分开。
					if (i > 10 && org.contains("parameter error")) {
						return "";
					}
					JSONArray objects = JSON.parseArray(org);
					String s_date = "";
					for (int j = 0; j < objects.size(); j++) {
						JSONObject data = objects.getJSONObject(j);
						String title = UnicodeUtil.UnicodeToCN(data.getString("title"));
						String date = data.getString("date");
//						System.err.println(date + " " + title);
						// 成功
						if (title.contains("上市公告书") || title.contains("发行情况报告书")) {
							// System.err.println("999999-endingggg:" + date + " " + type + " " + title);
							if (title.contains("资产") && title.contains("买")) {
								return title;
							}
						}
//						// 发行
//						if (title.contains("发行") && title.contains("案") && title.contains("股")) {
////							System.err.println("999999-startingggg:" + date + " " + type + " " + title);
//
//						}
						s_date = date;
					}
					fetched = true;
					int pageEndDate = 0;
					try {
						pageEndDate = DateUtil.convertDate2(s_date);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (pageEndDate < endDate) {
						return "";
					}
				} catch (Exception e2) {
					e2.printStackTrace();
					trytime++;
					ThreadsUtil.sleepRandomSecBetween1And5(trytime);
					if (trytime >= 10) {
						log.info("org:" + org);
						// log.info("UnicodeToCN:" + UnicodeToCN);
						fetched = true;
						MsgPushServer.pushToSystem("同花顺-抓包公告出错-抓包出错code=" + code + ",url=" + url);
					}
				} finally {
				}
			} while (!fetched);
			i++;
		}
	}

	public static int getLastAnn(String code) {
		try {
			String url = String.format(BASE_URL, code, 1);
			String org = HttpUtil.doGet2(url);
//		UnicodeToCN = UnicodeUtil.UnicodeToCN(org); 整个json有双引号的情况，所以要下面title分开。
			if (org.contains("parameter error")) {
				return 0;
			}
			JSONArray objects = JSON.parseArray(org);
			if (objects.size() > 0) {
				JSONObject data = objects.getJSONObject(0);
//			String title = UnicodeUtil.UnicodeToCN(data.getString("title"));
				String date = data.getString("date");
				return DateUtil.convertDate2(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void main(String[] args) {
//		String[] as = { "603385", "300676", "002405", "601369", "600789", "002612" };
		String[] as = { "002612" };
		for (int i = 0; i < as.length; i++) {
//			System.err.println(isBuyAsset(as[i], 20160101));
			System.err.println(getLastAnn(as[i]));
		}
	}

}
