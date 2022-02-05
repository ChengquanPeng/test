package com.stable.spider.eastmoney;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.RzrqDaliyDao;
import com.stable.es.dao.base.RztjDao;
import com.stable.service.DzjyService;
import com.stable.service.StockBasicService;
import com.stable.service.model.ModelWebService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.RzrqDaliy;
import com.stable.vo.bus.Rztj;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class RzrqSpider {

	// https://datacenter-web.eastmoney.com/api/data/get?callback=datatable577190&type=RPTA_WEB_RZRQ_GGMX&sty=ALL&source=WEB&p=2&ps=50&st=RZJME&sr=-1&filter=(date='2021-12-24')&pageNo=2&pageNum=2&pageNumber=2&_=1640587686687
	private static final String datatable577190 = "datatable577190(";
	private static String URL_S1 = "https://datacenter-web.eastmoney.com/api/data/get?callback=datatable577190&type=RPTA_WEB_RZRQ_GGMX&sty=ALL&source=WEB&filter=(date='%s')&p=%s&ps=50&st=RZJME&sr=-1&pageNo=%s&pageNum=%s&pageNumber=%s&_=%s";

	private static String URL_CODE_S1 = "https://datacenter-web.eastmoney.com/api/data/get?callback=datatable665034&type=RPTA_WEB_RZRQ_GGMX&sty=ALL&source=WEB&sr=-1&ps=50&filter=(scode%3D%22";
	private static String URL_CODE_S2 = "%22)";
	private static String URL_CODE_S3 = "&st=date&p=%s&pageNo=%s&pageNum=%s&pageNumber=%s&_=%s";

	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DzjyService dzjyService;
	@Autowired
	private ModelWebService modelWebService;
	@Autowired
	private RzrqDaliyDao rzrqDaliyDao;
	@Autowired
	private RztjDao rztjDao;

	public synchronized void byDaily(String dateYYYY_, int date) {
		Set<String> codes = new HashSet<String>();
		// setp1.get code
		List<RzrqDaliy> l = new LinkedList<RzrqDaliy>();
		int page = 1;
		while (true) {
			if (page >= getRzrqDaliys(page, dateYYYY_, l, codes)) {
				break;
			}
			page++;
			if (l.size() > 200) {
				rzrqDaliyDao.saveAll(l);
				l = new LinkedList<RzrqDaliy>();
			}
		}
		if (l.size() > 0) {
			rzrqDaliyDao.saveAll(l);
		}
		log.info("codes size:{}", codes.size());
		// STEP3:
		exeRzrqTime(codes, date);
		log.info("STEP3:done");
	}

	private double vaildLine = 30.0;// 超过平均数20%认为有效
	private double validBlance = 1.5 * CurrencyUitl.YI_N.doubleValue();// 1.5亿

	private synchronized void exeRzrqTime(Set<String> codes, int date) {
		int validDate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(DateUtil.parseDate(date), 20));// 有效期
		ThreadsUtil.sleepRandomSecBetween15And30();
		List<Rztj> l = new LinkedList<Rztj>();
		List<CodeBaseModel2> update = new LinkedList<CodeBaseModel2>();

		for (String code : codes) {
			Rztj rztj = dzjyService.getLastRztj(code);
			if (date > rztj.getValidDate()) {// 已失效
				rztj.setUpdateDate(date);
				if (dzjyService.rzrqAvg20d(code, vaildLine, validBlance, rztj)) {
					rztj.setValid(1);
					rztj.setValidDate(validDate);
				} else {
					rztj.setValid(0);
					rztj.setValidDate(0);
				}

				CodeBaseModel2 cbm = modelWebService.getLastOneByCode2(code);
				if (rztj.getValid() > 0) {
					cbm.setShooting3(1);
				} else {
					cbm.setShooting3(0);
				}

				l.add(rztj);
				if (l.size() > 200) {
					rztjDao.saveAll(l);
					l = new LinkedList<Rztj>();
				}
			}
		}

		if (l.size() > 0) {
			rztjDao.saveAll(l);
		}
		if (update.size() > 0) {
			codeBaseModel2Dao.saveAll(update);
		}
	}

	private int getRzrqDaliys(int page, String d, List<RzrqDaliy> list, Set<String> codes) {
		int pages = 9999;
		int trytime = 0;
		do {
			trytime++;
			try {
				String url = String.format(URL_S1, d, page, page, page, page, System.currentTimeMillis());
				log.info(url);
				String result = HttpUtil.doGet2(url);
				result = result.substring(datatable577190.length(), result.length() - 2);
				JSONObject object = JSON.parseObject(result).getJSONObject("result");
				pages = object.getInteger("pages");
				JSONArray objects = object.getJSONArray("data");
				for (int i = 0; i < objects.size(); i++) {
					JSONObject data = objects.getJSONObject(i);
					RzrqDaliy rd = getData(data, list);
					if (rd != null) {
						codes.add(rd.getCode());
					}
				}
				return pages;
			} catch (Exception e) {
				e.printStackTrace();
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
			}
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-融资融券(每日)-抓包出错,date=" + d);
		return pages;
	}

	public void byWeb(Callable<Boolean> back) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				dofetchInnerByAll();
				try {
					back.call();// 顺序执行
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void byWeb() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				dofetchInnerByAll();
			}
		}).start();
	}

	private RzrqDaliy getData(JSONObject data, List<RzrqDaliy> list) {
		try {
			String date = data.getString("DATE").substring(0, 10);
			// dzjy.setDate();
			String code = data.getString("SCODE");

			RzrqDaliy rzrq = new RzrqDaliy();
			rzrq.setCode(code);
			rzrq.setDate(DateUtil.convertDate2(date));
			rzrq.setId();

			rzrq.setBalance(data.getDouble("RZYE"));
			rzrq.setMr(data.getDouble("RZMRE"));
			rzrq.setMc(data.getDouble("RZCHE"));
			rzrq.setJmr(data.getDouble("RZJME"));
			list.add(rzrq);
			return rzrq;
//			String ye = CurrencyUitl.covertToString(data.getDouble("RZYE"));
//			String mr = CurrencyUitl.covertToString(data.getDouble("RZMRE"));
//			String mc = CurrencyUitl.covertToString(data.getDouble("RZCHE"));
//			String jmr = CurrencyUitl.covertToString(data.getDouble("RZJME"));
//			System.err.println(date + " " + code + " " + ye + " " + mr + " " + mc + " " + jmr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized void dofetchInnerByAll() {
		try {
			int date = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
			List<RzrqDaliy> dzl = new LinkedList<RzrqDaliy>();
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithOutSort();
			int c = 0;
			Set<String> codes = new HashSet<String>();
			for (StockBaseInfo s : codelist) {
				try {
					ThreadsUtil.sleepSleep1Seconds();
					codes.add(s.getCode());
					dofetchByCode(s.getCode(), dzl);
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "", "", "");
				}
				c++;
				log.info("current index:{}", c);

				if (dzl.size() > 200) {
					rzrqDaliyDao.saveAll(dzl);
					dzl = new LinkedList<RzrqDaliy>();
				}
			}
			if (dzl.size() > 0) {
				rzrqDaliyDao.saveAll(dzl);
			}
			exeRzrqTime(codes, date);
			WxPushUtil.pushSystem1(date + " 东方财富-融资融券-已完成-ALL");
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "东方财富-融资融券-运行异常..", "", "");
			WxPushUtil.pushSystem1("东方财富-融资融券-运行异常");
		}
	}

	private int maxpage = 10;// 最大抓取页数:太多了没用

	private void dofetchByCode(String code, List<RzrqDaliy> list) {
		int runPage = 1;
		int totPage = 0;
		while (true) {
			int trytime = 0;
			do {
				trytime++;
				try {
					String url = URL_CODE_S1 + code + URL_CODE_S2 + String.format(URL_CODE_S3, runPage, runPage,
							runPage, runPage, System.currentTimeMillis());
					log.info(url);
					String result = HttpUtil.doGet2(url);
					result = result.substring(datatable577190.length(), result.length() - 2);
					JSONObject object = JSON.parseObject(result);

					if (object.getBooleanValue("success")) {
						object = object.getJSONObject("result");
						totPage = object.getIntValue("pages");
						JSONArray objects = object.getJSONArray("data");
						for (int i = 0; i < objects.size(); i++) {
							getData(objects.getJSONObject(i), list);
						}
					} else {
						log.info("{} 未获取到融资融券", code);
						return;
					}
					break;// 获取完成，退出
				} catch (Exception e) {
					e.printStackTrace();
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					WxPushUtil.pushSystem1("东方财富-融资融券-抓包出错,code=" + code + ",page=" + runPage);
					return;
				}
			} while (trytime <= 10);

			runPage++;
			if (runPage > totPage && runPage > maxpage) {// 最大抓取页数:太多了没用
				break;
			}
		}
	}

	public static void main(String[] args) {

		RzrqSpider es = new RzrqSpider();
		es.byDaily("2022-01-04", 20220104);

//		List<RzrqDaliy> list = new LinkedList<RzrqDaliy>();
//		es.dofetchByCode("000728", list);
//		System.err.println(list.size());

	}

}
