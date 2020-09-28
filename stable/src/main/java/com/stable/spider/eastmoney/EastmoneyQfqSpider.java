package com.stable.spider.eastmoney;

import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EastmoneyQfqSpider {
	/**
	 * 格式化成东方财富 API所需格式
	 */
	public static String formatCode2(String code) {
		// 5开头，沪市基金或权证 60开头上证
		if (code.startsWith("6")) {
			return String.format("SH%s", code);
		} else {
			return String.format("SZ%s", code);
		}
	}

	public static int formatCode(String code) {
		if (code.startsWith("6")) {
			return 1;
		} else if (code.startsWith("0")) {
			return 0;
		} else if (code.startsWith("3")) {
			return 0;
		}
		return 0;
	}

	// 随机时间-复权类型-东方code-随机时间
	// 0不复权，1前复权，2后复权
	public synchronized static List<TradeHistInfoDaliy> getQfq(String code) {
		try {
			int qfqDate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
			Long systemtime = System.currentTimeMillis();
			String URL_FORMAT = "http://push2his.eastmoney.com/api/qt/stock/kline/get?cb=jQuery" + systemtime
					+ "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61&ut=7eea3edcaed734bea9cbfc24409ed989&klt=101&fqt="
					+ "1" + "&secid=" + formatCode(code) + "." + code + "&beg=0&end=20500000&_="
					+ System.currentTimeMillis();
			String result = HttpUtil.doGet2(URL_FORMAT);
			result = result.substring(("jQuery" + systemtime + "(").length());
			result = result.substring(0, result.length() - 2);
			JSONObject objects = JSON.parseObject(result);
			JSONArray datas = objects.getJSONObject("data").getJSONArray("klines");
			double yesterdayprice = 0.0;
			List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
			for (int i = 0; i < datas.size(); i++) {
				String data = datas.getString(i);
				TradeHistInfoDaliy td = new TradeHistInfoDaliy(code, data);
				td.setYesterdayPrice(yesterdayprice);
				yesterdayprice = td.getClosed();
				td.setQfqDate(qfqDate);
				list.add(td);
			}
			log.info("{} ->从东方财富获取前复权数据记录条数:{}", code, list.size());
			return list;
		} finally {
			ThreadsUtil.sleepRandomSecBetween1And5();
		}
	}

	public static void main(String[] args) {
//		String code = "000001";
		String code = "600000";
		getQfq(code);
	}
}
