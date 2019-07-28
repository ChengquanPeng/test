package com.stable.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.spider.tushare.TushareSpider;

@Service
public class FinanceService {

	@Autowired
	private TushareSpider tushareSpider;
	
	public void getFinaceHistoryInfo(String code) {
		JSONObject json = new JSONObject();
        //接口名称
        json.put("api_name","fina_indicator");
        //只取上市的
        json.put("params",JSON.parse("{'ts_code':'"+TushareSpider.formatCode(code)+"'}"));
        json.put("fields","ts_code,ann_date,end_date,profit_dedt,profit_to_gr,npta,dtprofit_to_profit");
        String result = tushareSpider.post(json);
        System.err.println(result);
        JSONObject datas= JSON.parseObject(result);
        JSONArray items =datas.getJSONObject("data").getJSONArray("items");
        System.err.println(items);
	}
	
	@PostConstruct
	private void test() {
		getFinaceHistoryInfo("600001");
	}
}
