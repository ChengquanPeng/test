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
	private String api = "http://api.waditu.com";

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
	private final String stock_basic_fields = "ts_code,symbol,name,area,industry,fullname,enname,market,exchange,curr_type,list_status,list_date,delist_date,is_hs";

	public JSONArray getStockCodeList() {
		JSONObject json = new JSONObject();
		json.put("api_name", "stock_basic");
		// 只取上市的
//		json.put("params", JSON.parse("{'list_status':'L'}"));
		json.put("fields", stock_basic_fields);
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
	private final String daily_fields = "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount";

	public JSONArray getStockDaliyTrade(String ts_code, String trade_date, String start_date, String end_date) {
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

			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} finally {
			ThreadsUtil.tuShareSleepRandom();
		}
	}

	/**
	 * 日线行情-每日指标
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return
	 */
	private final String stockdaliybasic_fields = "ts_code,trade_date,close,turnover_rate,turnover_rate_f,volume_ratio,pe,pe_ttm,pb,ps,ps_ttm,dv_ratio,dv_ttm,total_share,float_share,free_share,total_mv,circ_mv";

	public JSONObject getStockDaliyBasic(String ts_code, String trade_date) {
		try {
			StockDaliyReq req = new StockDaliyReq();
			req.setTrade_date(trade_date);
			req.setTs_code(ts_code);
//			req.setStart_date(start_date);  , String start_date, String end_date
//			req.setEnd_date(end_date);

			JSONObject json = new JSONObject();
			json.put("api_name", "daily_basic");
			json.put("params", JSON.parse(JSON.toJSONString(req)));
			json.put("fields", stockdaliybasic_fields);

			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONObject items = datas.getJSONObject("data");
			return items;
		} finally {
			// ThreadsUtil.tuShareSleepRandom();
		}
	}

	/**
	 * 交易日历
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return
	 */
	private final String trade_cal_fields = "cal_date,is_open,pretrade_date";

	public JSONArray getTradeCal(String start_date, String end_date) {
		try {
			JSONObject json = new JSONObject();
			json.put("api_name", "trade_cal");
			json.put("params", JSON.parse("{'start_date':'" + start_date + "','end_date':'" + end_date + "'}"));
			json.put("fields", trade_cal_fields);

			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} finally {
			ThreadsUtil.tuShareSleepRandom();
		}
	}

	/**
	 * 回购
	 * 
	 * @param ts_code    ts代码
	 * @param start_date 开始日期 (格式：YYYYMMDD)
	 * @param end_date   结束日期 (格式：YYYYMMDD)
	 * @return 如果都不填，单次默认返回2000条
	 */
	private final String buyback_fields = "ts_code,ann_date,end_date,proc,exp_date,vol,amount,high_limit,low_limit";

	public JSONArray getBuyBackList(String start_date, String end_date, String ann_date) {
		try {
			JSONObject json = new JSONObject();
			json.put("api_name", "repurchase");
			if (StringUtils.isNotBlank(start_date) && StringUtils.isNotBlank(end_date)) {
				json.put("params", JSON.parse("{'start_date':'" + start_date + "','end_date':'" + end_date + "'}"));
			} else if (StringUtils.isNotBlank(ann_date)) {
				json.put("params", JSON.parse("{'ann_date':'" + ann_date + "'}"));
			}
			json.put("fields", buyback_fields);

			String result = post(json);
			JSONObject datas = JSON.parseObject(result);
			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
			return items;
		} finally {
			ThreadsUtil.tuShareSleepRandom();
		}
	}

//	/**
//	 * 质押统计
//	 * 
//	 * @param ts_code  ts代码
//	 * @param end_date 结束日期 (格式：YYYYMMDD)
//	 */
//	private final String pledge_stat_fields = "ts_code,end_date,pledge_count,unrest_pledge,rest_pledge,total_share,pledge_ratio";
//
//	public JSONArray getPledgeStatList(String ts_code, String end_date) {
//		try {
//			JSONObject json = new JSONObject();
//			json.put("api_name", "pledge_stat");
//			json.put("params", JSON.parse("{'ts_code':'" + ts_code + "','end_date':'" + end_date + "'}"));
//			json.put("fields", pledge_stat_fields);
//
//			String result = post(json);
//			JSONObject datas = JSON.parseObject(result);
//			JSONArray items = datas.getJSONObject("data").getJSONArray("items");
//			return items;
//		} finally {
//			ThreadsUtil.tuShareSleepRandom();
//		}
//	}

}
