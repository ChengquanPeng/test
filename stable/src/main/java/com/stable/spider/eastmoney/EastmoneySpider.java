package com.stable.spider.eastmoney;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.HttpUtil;
import com.stable.utils.TickDataUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.TickData;

@Component
public class EastmoneySpider {
	private static final String URL_FORMAT = "https://push2.eastmoney.com/api/qt/stock/details/get?secid=%s.%s&fields1=f1,f2,f3,f4,f5&fields2=f51,f52,f53,f54,f55&pos=-111125&";

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

	public synchronized static List<String> getRealtimeTickByJob(String code) {
		try {
			int mk = EastmoneySpider.formatCode(code);
			JSONObject result = HttpUtil.doGet(String.format(URL_FORMAT, mk, code));
			JSONObject data = (JSONObject) result.get("data");
			JSONArray details = (JSONArray) data.get("details");
			List<String> list = new LinkedList<String>();
			for (int i = 0; i < details.size(); i++) {
				String line = details.get(i).toString();
				if (i <= 50) {
					TickData d = TickDataUitl.getDataObjectFromEasymoney(line);
					// 排除集合竞价
					if (Integer.valueOf(d.getTime().replaceAll(":", "")) >= 92500) {
						list.add(line);
					}
				} else {
					list.add(line);
				}
			}
			if (list.size() < 20) {
				return null;
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
		}
	}

	public static List<TickData> getRealtimeTick(String code) {
		try {
			int mk = EastmoneySpider.formatCode(code);
			JSONObject result = HttpUtil.doGet(String.format(URL_FORMAT, mk, code));
			JSONObject data = (JSONObject) result.get("data");
			JSONArray details = (JSONArray) data.get("details");
			List<TickData> list = new LinkedList<TickData>();
			for (int i = 0; i < details.size(); i++) {
				String line = details.get(i).toString();
				TickData d = TickDataUitl.getDataObjectFromEasymoney(line);
				if (i <= 100) {
					// 排除集合竞价
					if (Integer.valueOf(d.getTime().replaceAll(":", "")) >= 92500) {
						list.add(d);
					}
				} else {
					list.add(d);
				}
			}
			if (list.size() > 0) {
				list.get(0).setType(TickDataUitl.UN);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("东方财富获取分笔失败:code=" + code);
			return Collections.emptyList();
		}
	}

	/**
	 * 财务信息
	 * 
	 * @param code 6位普通股票代码
	 * @param type 0按报告期、1=年报
	 * @return http://f10.eastmoney.com/NewFinanceAnalysis/MainTargetAjax?type=1&code=SZ300750
	 */
	static final String financeUrl = "http://f10.eastmoney.com/NewFinanceAnalysis/MainTargetAjax?type=%s&code=%s";

	public static List<FinanceBaseInfo> getNewFinanceAnalysis(String code, int type) {
		List<FinanceBaseInfo> list = new ArrayList<>();
		String url = String.format(financeUrl, type, formatCode2(code));
		String result = HttpUtil.doGet2(url);
		JSONArray objects = JSON.parseArray(result);
		for (int i = 0; i < objects.size(); i++) {
			JSONObject data = objects.getJSONObject(i);
			String date = data.get("date").toString(); // 年报日期
			FinanceBaseInfo newFinanceAnalysis = new FinanceBaseInfo(code, Integer.valueOf(date.replaceAll("-", "")));
			try {
				Double yyzsrtbzz = data.getDouble("yyzsrtbzz"); // 营业总收入同比增长(%)
				newFinanceAnalysis.setYyzsrtbzz(yyzsrtbzz);
			} catch (Exception e) {
			}
			try {
				Double gsjlrtbzz = data.getDouble("gsjlrtbzz"); // 归属净利润同比增长(%)
				newFinanceAnalysis.setGsjlrtbzz(gsjlrtbzz);
			} catch (Exception e) {
			}
			try {
				Double kfjlrtbzz = data.getDouble("kfjlrtbzz"); // 扣非净利润同比增长(%)
				newFinanceAnalysis.setKfjlrtbzz(kfjlrtbzz);
			} catch (Exception e) {
			}

			try {
				Long yyzsr = CurrencyUitl.covertToLong(data.get("yyzsr").toString()); // 营业总收入
				newFinanceAnalysis.setYyzsr(yyzsr);
			} catch (Exception e) {
			}
			try {
				Long gsjlr = CurrencyUitl.covertToLong(data.get("gsjlr").toString()); // 归属净利润
				newFinanceAnalysis.setGsjlr(gsjlr);
			} catch (Exception e) {
			}
			try {
				Long kfjlr = CurrencyUitl.covertToLong(data.get("kfjlr").toString()); // 扣非净利润同比增长(%)
				newFinanceAnalysis.setKfjlr(kfjlr);
			} catch (Exception e) {
			}
			list.add(newFinanceAnalysis);
		}
		return list;
	}

	public static void main(String[] args) {
		EastmoneySpider.getNewFinanceAnalysis("000002", 0);
	}
}
