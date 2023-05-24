package com.stable.spider.eastmoney;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.Constant;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.vo.bus.StockBaseInfo;

@Component
public class StockListSpider {
	private static String START = "jQuery11230025078677162053697_1684903266066";
	private static String url = "https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery11230025078677162053697_1684903266066&sortColumns=APPLY_DATE%2CSECURITY_CODE&sortTypes=-1%2C-1&pageSize=50&pageNumber=1&reportName=RPTA_APP_IPOAPPLY&columns=SECURITY_CODE%2CSECURITY_NAME%2CTRADE_MARKET_CODE%2CAPPLY_CODE%2CTRADE_MARKET%2CMARKET_TYPE%2CORG_TYPE%2CISSUE_NUM%2CONLINE_ISSUE_NUM%2COFFLINE_PLACING_NUM%2CTOP_APPLY_MARKETCAP%2CPREDICT_ONFUND_UPPER%2CONLINE_APPLY_UPPER%2CPREDICT_ONAPPLY_UPPER%2CISSUE_PRICE%2CLATELY_PRICE%2CCLOSE_PRICE%2CAPPLY_DATE%2CBALLOT_NUM_DATE%2CBALLOT_PAY_DATE%2CLISTING_DATE%2CAFTER_ISSUE_PE%2CONLINE_ISSUE_LWR%2CINITIAL_MULTIPLE%2CINDUSTRY_PE_NEW%2COFFLINE_EP_OBJECT%2CCONTINUOUS_1WORD_NUM%2CTOTAL_CHANGE%2CPROFIT%2CLIMIT_UP_PRICE%2CINFO_CODE%2COPEN_PRICE%2CLD_OPEN_PREMIUM%2CLD_CLOSE_CHANGE%2CTURNOVERRATE%2CLD_HIGH_CHANG%2CLD_AVERAGE_PRICE%2COPEN_DATE%2COPEN_AVERAGE_PRICE%2CPREDICT_PE%2CPREDICT_ISSUE_PRICE2%2CPREDICT_ISSUE_PRICE%2CPREDICT_ISSUE_PRICE1%2CPREDICT_ISSUE_PE%2CPREDICT_PE_THREE%2CONLINE_APPLY_PRICE%2CMAIN_BUSINESS%2CPAGE_PREDICT_PRICE1%2CPAGE_PREDICT_PRICE2%2CPAGE_PREDICT_PRICE3%2CPAGE_PREDICT_PE1%2CPAGE_PREDICT_PE2%2CPAGE_PREDICT_PE3%2CSELECT_LISTING_DATE%2CIS_BEIJING%2CINDUSTRY_PE_RATIO%2CINDUSTRY_PE%2CIS_REGISTRATION&quoteColumns=f2~01~SECURITY_CODE~NEWEST_PRICE&quoteType=0&filter=(APPLY_DATE%3E%272010-01-01%27)&source=WEB&client=WEB";

	public static void main(String[] args) {

	}

	public List<StockBaseInfo> getStockList() {
		List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
		try {
			String result = HttpUtil.doGet2(url);
			// System.err.println(result);
			result = result.substring(START.length() + 1, result.length() - 2);
			// System.err.println(result);
			JSONObject object = JSON.parseObject(result);
			JSONObject rlt = object.getJSONObject("result");
			JSONArray data = rlt.getJSONArray("data");
			for (int i = 0; i < data.size(); i++) {
				JSONObject row = data.getJSONObject(i);
				StockBaseInfo base = new StockBaseInfo();
				base.setCode(row.getString("SECURITY_CODE"));
				base.setName(row.getString("SECURITY_NAME"));
				base.setList_date(getListDate(row.getString("LISTING_DATE")));
				base.setMarket(getMaketcode(row.getString("TRADE_MARKET")));
				base.setList_status(Constant.CODE_ON_STATUS);
				// System.err.println(base);
				list.add(base);
			}
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "新股同步异常", "", "");
			MsgPushServer.pushSystem1("新股同步异常");
			e.printStackTrace();
		}
		return list;
	}

	private static String getListDate(String date) {
		if (StringUtils.isNotBlank(date)) {
			return DateUtil.getDateStrToIntYYYYMMDDHHMMSS(date) + "";
		}
		return "0";
	}

	private static String getMaketcode(String s) {
		if (s.contains("上海")) {
			return "1";
		}
		if (s.contains("深圳")) {
			return "2";
		}
		if (s.contains("北京")) {
			return "3";
		}
		return "0";
	}
}
