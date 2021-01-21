package com.stable.spider.eastmoney;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;

//**
//质押统计
//**
//@Component
public class EastmoneyZytjSpider {

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

	/**
	 * 财务信息
	 * 
	 * @param code 6位普通股票代码
	 * @param type 0按报告期、1=年报
	 * @return http://f10.eastmoney.com/NewFinanceAnalysis/MainTargetAjax?type=1&code=SZ300750
	 */
	static final String financeUrl = "http://f10.eastmoney.com/CompanyBigNews/GetPledgeHolder?code=%s&pageIndex=%s&t=%s";

	public static Map<String, Zyi> getZy(String code) {
		Map<String, Zyi> m = new HashMap<String, Zyi>();
		int trytime = 0;
		int i = 1;
		do {
			boolean fetch = false;
			do {
				trytime++;
				try {
					String url = String.format(financeUrl, formatCode2(code), i, System.currentTimeMillis());
					System.err.println(url);
					String result = HttpUtil.doGet2(url);
//					System.err.println(result);
					JSONArray objects = JSON.parseArray(result);
					if (objects == null || objects.size() <= 0) {
						// 未ok、
						return m;
					}
					fetch = true;
					int isEnd = 0;
					for (int k = 0; k < objects.size(); k++) {
						JSONObject data = objects.getJSONObject(k);
						int zyjzr = 0;
						try {
							zyjzr = DateUtil.convertDate2(data.getString("zyjzr"));
						} catch (Exception e) {
						}
						if (zyjzr == 0) {
							String gdmc = data.getString("gdmc");
							double zgdcgbl = Double.valueOf(data.getString("zgdcgbl").replace("%", ""));// 股東比例
							Zyi total = m.get(gdmc);
							if (total == null) {
								total = new Zyi();
								m.put(gdmc, total);
							}
							total.setC(total.getC() + 1);
							total.setBi(total.getBi() + zgdcgbl);
						} else {
							isEnd++;
						}
					}
//					if (isEnd == objects.size()) {
//						return m;
//					}
					if(i>30) {
						return m;
					}
					i++;
				} catch (Exception e) {
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					if (trytime >= 10) {
						WxPushUtil.pushSystem1("东方财富-财务(资产负债表)-抓包出错,code=" + code);
						fetch = true;
						return m;
					} else {
						e.printStackTrace();
					}
				}
			} while (!fetch);

		} while (true);
	}

	// http://data.eastmoney.com/bbsj/202003/yjyg.html
	// http://data.eastmoney.com/bbsj/yjyg/000662.html
	// private final String yjygBase =
	// "http://data.eastmoney.com/bbsj/yjyg/%s.html";

	public static void main(String[] args) {
		Map<String, Zyi> m = EastmoneyZytjSpider.getZy("300027");
		for (String key : m.keySet()) {
			Zyi z = m.get(key);
			System.err.println(key + "-> ci:" + z.getC() + "bili:" + z.getBi() + "%");
		}
//		System.err.println(CurrencyUitl.roundHalfUp( 5.9548019532E8/4.56697178601E9));
	}
}
