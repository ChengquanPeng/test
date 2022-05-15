package com.stable.spider.eastmoney;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.JiejinDao;
import com.stable.service.StockBasicService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class JiejinSpider {

	// http://data.eastmoney.com/dxf/q/601989.html

	private static final String J_QUERY112306735504837667934_1610722345186 = "jQuery112306735504837667934_1610722345186(";
	private String URL_S = "http://dcfm.eastmoney.com/em_mutisvcexpandinterface/api/js/get?callback=jQuery112306735504837667934_1610722345186&st=ltsj&sr=-1&ps=50&p=1&token=70f12f2f4f091e459a279469fe49eca5&type=XSJJ_NJ_PC&js=%7B%22data%22%3A(x)%2C%22pages%22%3A(tp)%2C%22font%22%3A(font)%7D&filter=(gpdm%3D%27";
	private String URL_E = "%27)";
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private JiejinDao jiejinDao;

	public void dofetch() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithSort();
					List<Jiejin> savelist = new ArrayList<Jiejin>();
					for (StockBaseInfo b : list) {

						dofetch(b.getCode(), savelist);
						ThreadsUtil.sleepRandomSecBetween1And5();
						if (savelist.size() > 100) {
							jiejinDao.saveAll(savelist);
							savelist = new ArrayList<Jiejin>();
						}
					}
					if (savelist.size() > 0) {
						jiejinDao.saveAll(savelist);
					}
					WxPushUtil.pushSystem1("东方财富-抓包解禁完成");
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("东方财富-抓包解禁出错-抓包出错");
				}
			}
		}).start();
	}

	private void dofetch(String code, List<Jiejin> savelist) {
		int trytime = 0;
		do {
			trytime++;
			try {
				String url = URL_S + code + URL_E;
//				log.info(url);
				String result = HttpUtil.doGet2(url);
				result = result.substring(J_QUERY112306735504837667934_1610722345186.length(), result.length() - 1);
				JSONObject object = JSON.parseObject(result);
				JSONArray objects = object.getJSONArray("data");
				for (int i = 0; i < objects.size(); i++) {
					Jiejin jj = new Jiejin();
					JSONObject data = objects.getJSONObject(i);
					jj.setCode(data.getString("gpdm"));
					String date = data.getString("ltsj").substring(0, 10);
					// System.err.println(date);
					jj.setDate(DateUtil.convertDate2(date));
					jj.setType(data.getString("xsglx"));
					try {
						Double zb = data.getDouble("zb"); // 占比
						jj.setZb(CurrencyUitl.roundHalfUp(zb * 100));
					} catch (Exception e) {
					}
					try {
						Double zzb = data.getDouble("zzb"); // 总占比
						jj.setZzb(CurrencyUitl.roundHalfUp(zzb * 100));
					} catch (Exception e) {
					}
					jj.setId(jj.getCode() + jj.getDate());
					log.info(jj);
					savelist.add(jj);
				}
				return;
			} catch (Exception e) {
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				e.printStackTrace();
			}
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-解禁-抓包出错,code=" + code);
	}

	public static void main(String[] args) {
		JiejinSpider tp = new JiejinSpider();
		String[] as = { "601989", "603385", "300676", "002405", "601369", "600789", "002612" };
		for (int i = 0; i < as.length; i++) {
			tp.dofetch(as[i], new ArrayList<Jiejin>());
		}
	}

}
