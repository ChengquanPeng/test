package com.stable.spider.xq;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.Constant;
import com.stable.service.StockBasicService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.vo.bus.StockBaseInfo;

@Service
public class StockSyn {
	private String url = "https://ig507.com/data/base/gplist?";

	@Autowired
	private StockBasicService stockBasicService;

	public void stockListChk() {
		try {
			List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
			String date = DateUtil.getTodayYYYYMMDD();
			// System.err.println(url);
			String rest = HttpUtil.doGet2(url);
			// System.err.println(rest);
			JSONArray data = JSON.parseArray(rest);
			String code = null;
			String name = null;
			StockBaseInfo t = null;
			for (int i = 0; i < data.size(); i++) {
				JSONObject row = data.getJSONObject(i);
				code = row.getString("dm");
				name = row.getString("mc");

				// 是否退市股票
				if (stockBasicService.isTuiShi(name)) {
					if (!stockBasicService.isTuiShi(t.getName())) {// 同步数据显示已经退市，但本系统不是退市，则更新
						stockBasicService.synName(code, name);
					}
				} else {
					t = stockBasicService.getCode(code);
					if (t.getCode().equals(StockBasicService.NO)) {// 非退市股票，但是本系统不存在
						MsgPushServer.pushSystem1("发现新未同步的股票：" + code + name);

						StockBaseInfo base = new StockBaseInfo();
						base.setCode(row.getString("SECURITY_CODE"));
						base.setName(row.getString("SECURITY_NAME"));
						base.setList_date(date);
						base.setMarket(stockBasicService.getMaketcode(row.getString("TRADE_MARKET")));
						base.setList_status(Constant.CODE_ON_STATUS);
						// System.err.println(base);
						list.add(base);
					}
				}
			}
			stockBasicService.saveAll(list);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "所有同步异常", "", "");
			MsgPushServer.pushSystem1("所有同步异常");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new StockSyn().stockListChk();
	}
}
