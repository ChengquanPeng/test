package com.stable.spider.xq;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.HttpUtil;
import com.stable.vo.bus.TradeHistInfoDaliy;

public class DailyFetchZx {
	private static final String _LOCAL_W = "999123";
	private static final String _LOCAL_Y = "888123";

	public String getCodeFmt(String code) {
		if (code.startsWith("6")) {
			return code;
		} else {
			return "SZ:" + code;
		}
	}

	// 中信日K
	public void zxRK(String code) {
		Map<String, String> p = new HashMap<String, String>();
		p.put("c.funcno", "20000");
		p.put("c.stock_list", getCodeFmt(code));
		p.put("c.field", "24:22:1:2:3:9:6:12:14:8:10:11:18:19:31:13:16:27");

		String rlt1 = HttpUtil.doPost3(ZXStockSyn.url, p);

		JSONObject arlt = JSON.parseObject(rlt1);
		String rlst2 = arlt.getString("BINDATA");
		System.err.println(rlst2);
		rlst2 = rlst2.replace(CurrencyUitl.WAN, _LOCAL_W).replace(CurrencyUitl.YI, _LOCAL_Y).replace(DailyFetch.SHOU,
				"");
		System.err.println(rlst2);
		JSONObject rlst3 = JSON.parseObject(rlst2);
		JSONArray data = rlst3.getJSONArray("results");
		for (int i = 0; i < data.size(); i++) {
			JSONArray row = data.getJSONArray(i);
			TradeHistInfoDaliy td = new TradeHistInfoDaliy();
			td.setCode(row.getString(0));
//				row.getString(1);// 名字
			td.setTodayChangeRate(row.getDoubleValue(2) * 100);// 涨跌幅
			td.setClosed(row.getDoubleValue(3));
			td.setTodayChange(row.getDoubleValue(4));
			td.setOpen(row.getDoubleValue(5));
//			td.setVolume(row.getDoubleValue(6));
			String s = row.getString(6);
			if (s.endsWith(_LOCAL_W)) {
				td.setVolume(Double.valueOf(s.replace(_LOCAL_W, "")) * 100 * 10000);
			} else if (s.endsWith(_LOCAL_Y)) {
				td.setVolume(Double.valueOf(s.replace(_LOCAL_W, "")) * 100 * 10000 * 10000);
			} else {// 只有手
				td.setVolume(Double.valueOf(s) * 100);
			}

			td.setYesterdayPrice(row.getDoubleValue(7));
			td.setAmt(row.getDoubleValue(8));
			td.setChangeHands(row.getDoubleValue(9) * 100);
			td.setHigh(row.getDoubleValue(10));
			td.setLow(row.getDoubleValue(11));
			td.setDate(999);
			td.setId();
			System.err.println("成交额：" + CurrencyUitl.covertToString(row.getDouble(8)));
			System.err.println("总市值：" + CurrencyUitl.covertToString(row.getLong(14) / 10000));
			System.err.println("市盈率（动）：" + row.getDoubleValue(15));
			System.err.println("振幅：" + row.getDoubleValue(16) * 100);
			System.err.println("流通市值：" + CurrencyUitl.covertToString(row.getLong(17) / 10000));
			System.err.println(td.toString());
		}
	}

	public static void main(String[] args) {
		DailyFetchZx x = new DailyFetchZx();
		String code = "300008";
		x.zxRK(code);
	}
}
