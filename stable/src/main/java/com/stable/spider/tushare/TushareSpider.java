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
import com.stable.utils.TheadUtil;
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
	private String api = "http://api.waditu.com";

	/**
	 * 格式化成tushare API所需格式
	 * 
	 * @param code
	 * @return
	 */
	public static String formatCode(String code) {
		// 5开头，沪市基金或权证 60开头上证
		if (code.startsWith("6")) {
			return String.format("%s.SH", code);
		} else if (code.startsWith("0")) {
			return String.format("%s.SZ", code);
		} else if (code.matches("^60.*|^5.*")) {
			return String.format("%s.SH", code);
		}
		// 1开头的，是深市基金 00开头是深圳
		else if (code.matches("^1.*|^00.*|^300...")) {
			return String.format("%s.SZ", code);
		}
		return null;
	}
	public static String removets(String tscode) {
		return tscode.substring(0, tscode.indexOf('.'));
	}
	public static void main(String[] args) {
		System.err.println(removets("300068.SZ"));
	}

	/**
	 * post 方式提交
	 * 
	 * @param params
	 * @return
	 */
	public String post(JSONObject params) {
		HttpHeaders headers = new HttpHeaders();
		// 定义请求参数类型，这里用json所以是MediaType.APPLICATION_JSON
		headers.setContentType(MediaType.APPLICATION_JSON);
		params.put("token", tuToken);
		HttpEntity<String> formEntity = new HttpEntity<String>(params.toString(), headers);
		String result = restTemplate.postForObject(api, formEntity, String.class);
		return result;
	}

	/**
	 * 返回已上市的A股代码
	 * 
	 * @return TS代码,股票代码,股票名称,所在地域,所属行业,股票全称,市场类型 （主板/中小板/创业板）,上市状态： L上市 D退市
	 *         P暂停上市,上市日期
	 */
	public JSONArray getStockCodeList() {
		JSONObject json = new JSONObject();
		// 接口名称
		json.put("api_name", "stock_basic");
		// 只取上市的
		json.put("params", JSON.parse("{'list_status':'L'}"));
		json.put("fields",
				"ts_code,symbol,name,area,industry,fullname,enname,market,exchange,curr_type,list_status,list_date,delist_date,is_hs");
		String result = post(json);
		JSONObject datas = JSON.parseObject(result);
		JSONArray items = datas.getJSONObject("data").getJSONArray("items");
		return items;
	}

	/**
	 * 获取上海公司基础信息
	 * 
	 * @return
	 */
	public JSONArray getStockShCompany() {
		JSONObject json = new JSONObject();
		// 接口名称
		json.put("api_name", "stock_company");
		json.put("params", JSON.parse("{'exchange':'SSE'}"));
		json.put("fields",
				"ts_code,chairman,manager,secretary,reg_capital,setup_date,province,city,introduction,website,email,office,employees,main_business,business_scope");
		String result = post(json);
		JSONObject data = JSON.parseObject(result);
		JSONArray items = data.getJSONObject("data").getJSONArray("items");
		return items;
	}

	/**
	 * 获取深圳公司基础信息
	 * 
	 * @return
	 */
	public JSONArray getStockSZCompany() {
		JSONObject json = new JSONObject();
		// 接口名称
		json.put("api_name", "stock_company");
		json.put("params", JSON.parse("{'exchange':'SZSE'}"));
		json.put("fields",
				"ts_code,chairman,manager,secretary,reg_capital,setup_date,province,city,introduction,website,email,office,employees,main_business,business_scope");
		String result = post(json);
		JSONObject data = JSON.parseObject(result);
		JSONArray items = data.getJSONObject("data").getJSONArray("items");
		return items;
	}

	/**
	 * 得到前10大持有人
	 * 
	 * @param code
	 * @return
	 */
	public JSONArray getStockTopHolders(String code) {
		JSONObject json = new JSONObject();
		// 接口名称
		json.put("api_name", "top10_holders");
		json.put("params", JSON.parse(String.format("{'ts_code':'%s'}", code)));
		json.put("fields", "ts_code,ann_date,end_date,holder_name,hold_amount,hold_ratio");
		String result = post(json);
		JSONObject datas = JSON.parseObject(result);
		JSONArray items = datas.getJSONObject("data").getJSONArray("items");
		return items;
	}

	/**
	 * 日线行情
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return
	 */
	public JSONArray getStockDaliyTrade(String ts_code,String trade_date,  String start_date, String end_date) {
		try {
			StockDaliyReq req = new StockDaliyReq();
			if (StringUtils.isNotBlank(trade_date)) {
				req.setTrade_date(trade_date);
			} else {
				req.setTs_code(ts_code);
				req.setStart_date(start_date);
				req.setEnd_date(end_date);
			}

			JSONObject json = new JSONObject();
			// 接口名称
			json.put("api_name", "daily");
			// 只取上市的
			json.put("params", JSON.parse(JSON.toJSONString(req)));

			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} finally {
			TheadUtil.sleepRandomSecBetween1And5();
		}
	}

	/**
	 * 日线行情
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return
	 */
	public JSONArray getStockDaliyBasic(String ts_code, String trade_date, String start_date, String end_date) {
		try {
			StockDaliyReq req = new StockDaliyReq();
			if (StringUtils.isNotBlank(trade_date)) {
				req.setTrade_date(trade_date);
			} else {
				req.setTs_code(ts_code);
				req.setStart_date(start_date);
				req.setEnd_date(end_date);
			}

			JSONObject json = new JSONObject();
			// 接口名称
			json.put("api_name", "daily_basic");
			// 只取上市的
			json.put("params", JSON.parse(JSON.toJSONString(req)));

			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} finally {
			TheadUtil.sleepRandomSecBetween1And5();
		}
	}
	
	/**
	 * 日线行情
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return
	 */
	public JSONArray getTradeCal(String start_date, String end_date) {
		try {
			JSONObject json = new JSONObject();
			json.put("api_name", "trade_cal");
			json.put("params", JSON.parse("{'start_date':'"+start_date+"','end_date':'"+end_date+"','is_open':'1'}"));
			json.put("fields", "cal_date,pretrade_date");
			
			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} finally {
			TheadUtil.sleepRandomSecBetween1And5();
		}
	}
}
