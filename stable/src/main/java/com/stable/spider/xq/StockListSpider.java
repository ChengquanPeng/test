package com.stable.spider.xq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.constant.Constant;
import com.stable.service.StockBasicService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockBaseInfo;

@Component
public class StockListSpider {
	private static String START = "jQuery11230025078677162053697_1684903266066";
	private static String url = "https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery11230025078677162053697_1684903266066&sortColumns=APPLY_DATE%2CSECURITY_CODE&sortTypes=-1%2C-1&pageSize=50&pageNumber=1&reportName=RPTA_APP_IPOAPPLY&columns=SECURITY_CODE%2CSECURITY_NAME%2CTRADE_MARKET_CODE%2CAPPLY_CODE%2CTRADE_MARKET%2CMARKET_TYPE%2CORG_TYPE%2CISSUE_NUM%2CONLINE_ISSUE_NUM%2COFFLINE_PLACING_NUM%2CTOP_APPLY_MARKETCAP%2CPREDICT_ONFUND_UPPER%2CONLINE_APPLY_UPPER%2CPREDICT_ONAPPLY_UPPER%2CISSUE_PRICE%2CLATELY_PRICE%2CCLOSE_PRICE%2CAPPLY_DATE%2CBALLOT_NUM_DATE%2CBALLOT_PAY_DATE%2CLISTING_DATE%2CAFTER_ISSUE_PE%2CONLINE_ISSUE_LWR%2CINITIAL_MULTIPLE%2CINDUSTRY_PE_NEW%2COFFLINE_EP_OBJECT%2CCONTINUOUS_1WORD_NUM%2CTOTAL_CHANGE%2CPROFIT%2CLIMIT_UP_PRICE%2CINFO_CODE%2COPEN_PRICE%2CLD_OPEN_PREMIUM%2CLD_CLOSE_CHANGE%2CTURNOVERRATE%2CLD_HIGH_CHANG%2CLD_AVERAGE_PRICE%2COPEN_DATE%2COPEN_AVERAGE_PRICE%2CPREDICT_PE%2CPREDICT_ISSUE_PRICE2%2CPREDICT_ISSUE_PRICE%2CPREDICT_ISSUE_PRICE1%2CPREDICT_ISSUE_PE%2CPREDICT_PE_THREE%2CONLINE_APPLY_PRICE%2CMAIN_BUSINESS%2CPAGE_PREDICT_PRICE1%2CPAGE_PREDICT_PRICE2%2CPAGE_PREDICT_PRICE3%2CPAGE_PREDICT_PE1%2CPAGE_PREDICT_PE2%2CPAGE_PREDICT_PE3%2CSELECT_LISTING_DATE%2CIS_BEIJING%2CINDUSTRY_PE_RATIO%2CINDUSTRY_PE%2CIS_REGISTRATION&quoteColumns=f2~01~SECURITY_CODE~NEWEST_PRICE&quoteType=0&filter=(APPLY_DATE%3E%272010-01-01%27)&source=WEB&client=WEB";

	@Autowired
	private StockBasicService stockBasicService;

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

				if (stockBasicService.isHuShenCode(base.getCode())) {
					base.setName(row.getString("SECURITY_NAME"));
					base.setList_date(getListDate(row.getString("LISTING_DATE")));
					if ("0".equals(base.getList_date())) {
						base.setList_status(Constant.CODE_STATUS_W);
					} else {
						base.setList_status(Constant.CODE_ON_STATUS);
					}
					base.setMarket(stockBasicService.getMaketcode(row.getString("TRADE_MARKET")));
					// System.err.println(base);
					list.add(base);
				}
			}
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "新股同步异常", "", "");
			MsgPushServer.pushToSystem("新股同步异常");
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

	private String urlths = "https://data.10jqka.com.cn/market/zdfph/field/zdf/order/desc/ajax/1/free/1/page/%d/free/1/";
	@Autowired
	private HtmlunitSpider htmlunitSpider;// = new HtmlunitSpider();

	public void getSynStockList() {
		boolean fetchedAll = false;
		int pageNum = 99;
		do {
			boolean fetched = false;
			int trytime = 0;
			do {
				ThreadsUtil.sleepRandomSecBetween1And2();
				HtmlPage page = null;
				HtmlElement body = null;
				try {
					String ut = String.format(urlths, pageNum);
					System.err.println(ut);
					page = htmlunitSpider.getHtmlPageFromUrl(ut);
					body = page.getBody();
					Iterator<HtmlElement> trs = body.getElementsByTagName("tr").iterator();
					int index = 1;
					while (trs.hasNext()) {
						if (index == 1 || index == 2) {
							continue;// header 跳过
						}
						Iterator<DomElement> tds = trs.next().getChildElements().iterator();
						String indexd = tds.next().asText();
						String code = tds.next().asText();
						String name = tds.next().asText();
						System.err.println(indexd + " " + code + " " + name);
						index++;
					}
					fetched = true;
				} catch (Exception e) {
					e.printStackTrace();
					trytime++;
					ThreadsUtil.sleepRandomSecBetween1And5(trytime * 10);
					if (trytime >= 3) {
						fetched = true;
						MsgPushServer.pushToSystem("同花顺同步股票错误,url=" + urlths);
					}
				}
			} while (!fetched);
			pageNum++;

			fetchedAll = true;
		} while (!fetchedAll);
	}

	public static void main(String[] args) {
		StockListSpider ss = new StockListSpider();
		ss.htmlunitSpider = new HtmlunitSpider();
		ss.getSynStockList();
//		System.err.println(HttpUtil
//				.doGet2("https://data.10jqka.com.cn/market/zdfph/field/zdf/order/desc/ajax/1/free/1/page/1/free/1/"));
	}
}
