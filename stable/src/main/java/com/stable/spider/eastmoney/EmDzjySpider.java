package com.stable.spider.eastmoney;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.Dzjy;

import lombok.extern.log4j.Log4j2;

//@Component
@Log4j2
public class EmDzjySpider {

	// http://data.eastmoney.com/dxf/q/601989.html

	private static final String J_QUERY112306735504837667934_1610722345186 = "jQuery112300768465300321155_1615975430563(";
	private static String URL_S = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?callback=jQuery112300768465300321155_1615975430563&st=TDATE&sr=-1&ps=50&p=1&js=%7Bpages%3A(tp)%2Cdata%3A(x)%7D&token=70f12f2f4f091e459a279469fe49eca5&type=DZJYXQ&filter=(SECUCODE%3D%27";
	private static String URL_E = "%27)";

	@SuppressWarnings("unused")
	public static Dzjy dofetch(String code) {
		int trytime = 0;
		do {
			trytime++;
			try {
				String url = URL_S + code + URL_E;
				log.info(url);
				String result = HttpUtil.doGet2(url);
				result = result.substring(J_QUERY112306735504837667934_1610722345186.length(), result.length() - 1);
				JSONObject object = JSON.parseObject(result);
				JSONArray objects = object.getJSONArray("data");
				for (int i = 0; i < objects.size(); i++) {
//						private String TDATE;
//						private String SECUCODE;
//						private String SNAME;
//						private String PRICE;
//						private String TVOL;
//						private String TVAL;
//						private String BUYERNAME;
//						private String SALESNAME;
//						private String RCHANGE;

					Dzjy dzjy = new Dzjy();
					JSONObject data = objects.getJSONObject(i);
					dzjy.setSECUCODE(data.getString("SECUCODE"));
					dzjy.setSNAME(data.getString("SNAME"));
					String date = data.getString("TDATE").substring(0, 10);
					// System.err.println(date);
					dzjy.setDate(DateUtil.convertDate2(date));
					dzjy.setPRICE(data.getString("PRICE"));
					dzjy.setTVOL(data.getString("TVOL"));
					dzjy.setTVAL(data.getString("TVAL"));
					dzjy.setBUYERNAME(data.getString("BUYERNAME"));
					dzjy.setSALESNAME(data.getString("SALESNAME"));
					dzjy.setRCHANGE(data.getString("RCHANGE"));
					log.info(dzjy);
					return dzjy;
				}
			} catch (Exception e) {
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				e.printStackTrace();
			}
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-大宗交易-抓包出错,code=" + code);
		return new Dzjy();
	}

	public static void main(String[] args) {
		String[] as = { "601989" };
		for (int i = 0; i < as.length; i++) {
			EmDzjySpider.dofetch(as[i]);
		}
	}

}
