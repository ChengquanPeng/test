package com.stable.spider.eastmoney;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.TickDataUitl;
import com.stable.utils.WxPushUtil;
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

	public synchronized static List<TickData> getReallyTick(String code) {
		ThreadsUtil.thsSleepRandom();
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

	public static void main(String[] args) {
		EastmoneySpider.getReallyTick("603456");
	}
}
