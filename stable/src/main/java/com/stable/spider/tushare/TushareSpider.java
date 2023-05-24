package com.stable.spider.tushare;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.spi.req.StockDaliyReq;

/**
 * 用挖地免的接口
 **/
@Component("TushareSpider")
public class TushareSpider {

	@Value("${tushare.token}")
	private String tuToken;
	@Autowired
	private RestTemplate restTemplate;
	// private final String api = "http://api.tushare.pro";
	@Value("${tushare.api}")
	private String api = "http://api.tushare.pro";

	/**
	 * 格式化成tushare API所需格式
	 * 
	 */
	public static String formatCode(String code) {
		if (code.startsWith("6")) {// 6
			return String.format("%s.SH", code);
		} else if (code.startsWith("0") || code.matches("3")) {// 0,3
			return String.format("%s.SZ", code);
		} else if (code.startsWith("8") || code.startsWith("4")) {// 8,4
			return String.format("%s.BJ", code);
		} else if (code.matches("5")) {// 5开头，沪市基金或权证
			return String.format("%s.SH", code);
		} else if (code.matches("1")) {// 1开头的，是深市基金
			return String.format("%s.SZ", code);
		}
		return code;
	}

	public static String removets(String tscode) {
		return tscode.substring(0, tscode.indexOf('.'));
	}

	/**
	 * post 方式提交
	 * 
	 * @param params
	 * @return
	 */
	private String post(JSONObject params) {
		HttpHeaders headers = new HttpHeaders();
		// 定义请求参数类型，这里用json所以是MediaType.APPLICATION_JSON
		headers.setContentType(MediaType.APPLICATION_JSON);
		params.put("token", tuToken);
		HttpEntity<String> formEntity = new HttpEntity<String>(params.toString(), headers);
		String result = restTemplate.postForObject(api, formEntity, String.class);
		return result;
	}

	/**
	 * 日线行情
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return
	 */
	private final String daily_fields = "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount";

	public static void main(String[] args) {
		try {

			TushareSpider tushareSpider = new TushareSpider();
			String today = "20230523";
			tushareSpider.tuToken = "2daca7eae236547999666447c27869984817c650a0db32fb6a6e1dcb";
			tushareSpider.restTemplate = new RestTemplate();
			JSONArray array = tushareSpider.getStockDaliyTrade(null, today, null, null);
//			JSONArray array = tushareSpider.getStockCodeList();
			System.err.println(array);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JSONArray getStockDaliyTrade(String ts_code, String trade_date, String start_date, String end_date) {
		// 偶尔会空异常
		for (int j = 0; j < 3; j++) {
			try {
				return getStockDaliyTradeProxy(ts_code, trade_date, start_date, end_date);
			} catch (Exception e) {
				ThreadsUtil.sleepRandomSecBetween5And15();
			}
		}
		throw new RuntimeException("getStockDaliyTrade exception");
	}

	private JSONArray getStockDaliyTradeProxy(String ts_code, String trade_date, String start_date, String end_date) {
		String result = "";
		try {
			StockDaliyReq req = new StockDaliyReq();
			if (StringUtils.isNotBlank(ts_code)) {
				req.setTs_code(ts_code);
			}
			if (StringUtils.isNotBlank(trade_date)) {
				req.setTrade_date(trade_date);
			}
			if (StringUtils.isNotBlank(start_date)) {
				req.setStart_date(start_date);
			}
			if (StringUtils.isNotBlank(end_date)) {
				req.setEnd_date(end_date);
			}

			JSONObject json = new JSONObject();
			json.put("api_name", "daily");
			json.put("params", JSON.parse(JSON.toJSONString(req)));
			json.put("fields", daily_fields);

			result = post(json);

			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} catch (RuntimeException e) {
			System.err.println(result);
			e.printStackTrace();
			throw e;
		} finally {
			ThreadsUtil.tuShareSleepRandom();
		}
	}
}
