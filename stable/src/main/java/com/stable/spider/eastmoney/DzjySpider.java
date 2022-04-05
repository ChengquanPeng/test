package com.stable.spider.eastmoney;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.DzjyDao;
import com.stable.es.dao.base.DzjyYiTimeDao;
import com.stable.service.DzjyService;
import com.stable.service.StockBasicService;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DzjySpider {

	// http://data.eastmoney.com/dxf/q/601989.html

	private static final int NUM_10000 = 10000;
	private static final String J_QUERY112306735504837667934_1610722345186 = "jQuery11230594221138091166_1640497702907(";
	private static String URL_S1 = "https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery11230594221138091166_1640497702905&sortColumns=SECURITY_CODE&sortTypes=1&pageSize=50&pageNumber=";
	private static String URL_S2 = "&reportName=RPT_DATA_BLOCKTRADE&columns=TRADE_DATE%2CSECURITY_CODE%2CSECUCODE%2CSECURITY_NAME_ABBR%2CCHANGE_RATE%2CCLOSE_PRICE%2CDEAL_PRICE%2CPREMIUM_RATIO%2CDEAL_VOLUME%2CDEAL_AMT%2CTURNOVER_RATE%2CBUYER_NAME%2CSELLER_NAME%2CCHANGE_RATE_1DAYS%2CCHANGE_RATE_5DAYS%2CCHANGE_RATE_10DAYS%2CCHANGE_RATE_20DAYS%2CBUYER_CODE%2CSELLER_CODE&source=WEB&client=WEB&filter=(SECURITY_TYPE_WEB%3D%221%22)(TRADE_DATE%3D%27";
	private static String URL_E = "%27)";

	private static final String jQuery112303025386501906322_1640499199509 = "jQuery112303025386501906322_1640499199509(";
	private static String URL_CODE_S1 = "https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112303025386501906322_1640499199509&sortColumns=TRADE_DATE%2CDEAL_AMT&sortTypes=-1%2C-1&pageSize=50&pageNumber=";
	private static String URL_CODE_S2 = "&filter=(SECURITY_CODE%3D%22";
	private static String URL_CODE_S3 = "%22)&columns=ALL&source=WEB&client=WEB&reportName=RPT_DATA_BLOCKTRADE";

	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DzjyDao dzjyDao;
	@Autowired
	private DzjyYiTimeDao dzjyYiTimeDao;
	@Autowired
	private DzjyService dzjyService;

	public synchronized void byDaily(String dateYYYY_) {
		// setp1.get code
		Set<String> codes = new HashSet<String>();
		int page = 1;
		while (true) {
			if (page >= getCodes(page, dateYYYY_, codes)) {
				break;
			}
			page++;
		}
		log.info("codes size:{}", codes.size());
		if (codes.size() > 0) {
			// step2,从bycode 获取大宗
			List<Dzjy> dzl = new LinkedList<Dzjy>();
			int c = 0;
			for (String code : codes) {
				try {
					ThreadsUtil.sleepSleep1Seconds();
					dofetchByCode(code, dzl, false);
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "", "", "");
				}
				c++;
				log.info("current index:{}", c);
				if (dzl.size() > 100) {
					dzjyDao.saveAll(dzl);
					dzl = new LinkedList<Dzjy>();
				}
			}
			if (dzl.size() > 0) {
				dzjyDao.saveAll(dzl);
			}
		}
		// STEP3:计算-半年超1亿
		exeDzjyYiTime();
	}

	private void exeDzjyYiTime() {
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithOutSort();
		ThreadsUtil.sleepRandomSecBetween15And30();
		List<DzjyYiTime> l = new LinkedList<DzjyYiTime>();
		int startDate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -370));// 12个月
		int startDate2 = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -60));// 2个月
		for (StockBaseInfo s : codelist) {
			// 频繁统计
			DzjyYiTime t = dzjyService.halfOver1Yi(s, startDate, startDate2);// 12个月
			l.add(t);
		}

		if (l.size() > 0) {
			dzjyYiTimeDao.saveAll(l);
		}
	}

	private int getCodes(int page, String d, Set<String> codes) {
		int pages = 9999;
		int trytime = 0;
		do {
			trytime++;
			try {
				String url = URL_S1 + page + URL_S2 + d + URL_E;
				log.info(url);
				String result = HttpUtil.doGet2(url);
				result = result.substring(J_QUERY112306735504837667934_1610722345186.length(), result.length() - 2);
				JSONObject object = JSON.parseObject(result).getJSONObject("result");
				pages = object.getInteger("pages");
				JSONArray objects = object.getJSONArray("data");
				for (int i = 0; i < objects.size(); i++) {
					String code = objects.getJSONObject(i).getString("SECURITY_CODE");
					if (code != null && !"".equals(code.trim())) {
						codes.add(code);
					}
				}
				return pages;
			} catch (Exception e) {
				e.printStackTrace();
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
			}
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-大宗交易(每日)-抓包出错,date=" + d);
		return pages;

	}

	public void byWeb() {
		new Thread(new Runnable() {
			public void run() {
				try {
					dofetchInnerByAll();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void getData(JSONObject data, List<Dzjy> list) {
		try {
			Dzjy dzjy = new Dzjy();
			dzjy.setCode(data.getString("SECURITY_CODE"));
//			dzjy.setSNAME(data.getString("SNAME"));
			String date = data.getString("TRADE_DATE").substring(0, 10);
			// System.err.println(date);
			dzjy.setDate(DateUtil.convertDate2(date));
			dzjy.setPrice(Double.valueOf(data.getString("DEAL_PRICE")));
			dzjy.setTvol(Double.valueOf(data.getString("DEAL_VOLUME")) / NUM_10000);// 万股
			dzjy.setTval(Double.valueOf(data.getString("DEAL_AMT")) / NUM_10000);// 万元
			dzjy.setBuyername(data.getString("BUYER_NAME"));
			dzjy.setSalesname(data.getString("SELLER_NAME"));
			dzjy.setRchange(Double.valueOf(data.getString("PREMIUM_RATIO")));
			dzjy.setSaleCode(data.getString("SELLER_CODE"));
			dzjy.setBuyCode(data.getString("BUYER_CODE"));
			dzjy.setDailyRank(data.getIntValue("DAILY_RANK"));
			dzjy.setId();
			log.info(dzjy);
			list.add(dzjy);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void dofetchInnerByAll() {
		try {
			int date = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
			List<Dzjy> dzl = new LinkedList<Dzjy>();
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithOutSort();
			int c = 0;
			for (StockBaseInfo s : codelist) {
				try {
					ThreadsUtil.sleepSleep1Seconds();
					dofetchByCode(s.getCode(), dzl, true);
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "", "", "");
				}
				c++;
				log.info("current index:{}", c);

				if (dzl.size() > 100) {
					dzjyDao.saveAll(dzl);
					dzl = new LinkedList<Dzjy>();
				}
			}
			if (dzl.size() > 0) {
				dzjyDao.saveAll(dzl);
			}
			exeDzjyYiTime();
			WxPushUtil.pushSystem1(date + " 东方财富-大宗交易-已完成-ALL");
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "东方财富-大宗交易-运行异常..", "", "");
			WxPushUtil.pushSystem1("东方财富-大宗交易-运行异常");
		}
	}

	private void dofetchByCode(String code, List<Dzjy> list, boolean fetchAllPage) {
		int runPage = 1;
		int totPage = 0;
		while (true) {
			int trytime = 0;
			do {
				trytime++;
				try {
					String url = URL_CODE_S1 + runPage + URL_CODE_S2 + code + URL_CODE_S3;
					log.info(url);
					String result = HttpUtil.doGet2(url);
					result = result.substring(jQuery112303025386501906322_1640499199509.length(), result.length() - 2);
					JSONObject object = JSON.parseObject(result);

					if (object.getBooleanValue("success")) {
						object = object.getJSONObject("result");
						totPage = object.getIntValue("pages");
						JSONArray objects = object.getJSONArray("data");
						for (int i = 0; i < objects.size(); i++) {
							getData(objects.getJSONObject(i), list);
						}
					} else {
						log.info("{} 未获取到大宗交易", code);
						return;
					}
					break;// 获取完成，退出
				} catch (Exception e) {
					e.printStackTrace();
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					WxPushUtil.pushSystem1("东方财富-大宗交易-抓包出错,code=" + code + ",page=" + runPage);
					return;
				}
			} while (trytime <= 10);

			if (fetchAllPage) {// 抓取第一页
				runPage++;
				if (runPage > totPage) {
					break;
				}
			} else {
				break;
			}
		}
	}

	public static void main(String[] args) {

		DzjySpider es = new DzjySpider();
		es.byDaily("2021-12-24");

//		List<Dzjy> list = new LinkedList<Dzjy>();
//		es.dofetchByCode("601360", list, true);
//		System.err.println(list.size());
//		String t = "";
//		for (Dzjy r : list) {
//			System.out.println(r);
//			if (t.equals(r.getId())) {
//				System.err.println(t);
//			} else {
//				t = r.getId();
//			}
//
//		}

	}

}
