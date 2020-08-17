package com.stable.spider.eastmoney;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.RedisConstant;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.TickDataUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.TickData;

@Component
public class EastmoneySpider {
	private static final String URL_FORMAT = "https://push2.eastmoney.com/api/qt/stock/details/get?secid=%s.%s&fields1=f1,f2,f3,f4,f5&fields2=f51,f52,f53,f54,f55&pos=-111125&";

	@Autowired
	private RedisUtil redisUtil;

	public static int formatCode(String code) {
		if (code.startsWith("6")) {
			return 1;
		} else if (code.startsWith("0")) {
			return 0;
		} else if (code.startsWith("3")) {
			return 0;
		}
		return 0;
	}

	/**
	 * 格式化成东方财富 API所需格式
	 */
	public static String formatCode2(String code) {
		// 5开头，沪市基金或权证 60开头上证
		if (code.startsWith("6")) {
			return String.format("SH%s", code);
		} else {
			return String.format("SZ%s", code);
		}
	}

	public synchronized static List<String> getRealtimeTickByJob(String code) {
		try {
			int mk = EastmoneySpider.formatCode(code);
			JSONObject result = HttpUtil.doGet(String.format(URL_FORMAT, mk, code));
			JSONObject data = (JSONObject) result.get("data");
			JSONArray details = (JSONArray) data.get("details");
			List<String> list = new LinkedList<String>();
			for (int i = 0; i < details.size(); i++) {
				String line = details.get(i).toString();
				if (i <= 50) {
					TickData d = TickDataUitl.getDataObjectFromEasymoney(line);
					// 排除集合竞价
					if (Integer.valueOf(d.getTime().replaceAll(":", "")) >= 92500) {
						list.add(line);
					}
				} else {
					list.add(line);
				}
			}
			if (list.size() < 20) {
				return null;
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
		}
	}

	public static List<TickData> getRealtimeTick(String code) {
		try {
			int mk = EastmoneySpider.formatCode(code);
			JSONObject result = HttpUtil.doGet(String.format(URL_FORMAT, mk, code));
			JSONObject data = (JSONObject) result.get("data");
			JSONArray details = (JSONArray) data.get("details");
			List<TickData> list = new LinkedList<TickData>();
			for (int i = 0; i < details.size(); i++) {
				String line = details.get(i).toString();
				TickData d = TickDataUitl.getDataObjectFromEasymoney(line);
				if (i <= 100) {
					// 排除集合竞价
					if (Integer.valueOf(d.getTime().replaceAll(":", "")) >= 92500) {
						list.add(d);
					}
				} else {
					list.add(d);
				}
			}
			if (list.size() > 0) {
				list.get(0).setType(TickDataUitl.UN);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("东方财富获取分笔失败:code=" + code);
			return Collections.emptyList();
		}
	}

	/**
	 * 财务信息
	 * 
	 * @param code 6位普通股票代码
	 * @param type 0按报告期、1=年报
	 * @return http://f10.eastmoney.com/NewFinanceAnalysis/MainTargetAjax?type=1&code=SZ300750
	 */
	static final String financeUrl = "http://f10.eastmoney.com/NewFinanceAnalysis/MainTargetAjax?type=%s&code=%s";

	public static List<FinanceBaseInfo> getNewFinanceAnalysis(String code, int type) {
		List<FinanceBaseInfo> list = new ArrayList<>();
		String url = String.format(financeUrl, type, formatCode2(code));
		String result = HttpUtil.doGet2(url);
		JSONArray objects = JSON.parseArray(result);
		for (int i = 0; i < objects.size(); i++) {
			JSONObject data = objects.getJSONObject(i);
			String date = data.get("date").toString(); // 年报日期
			FinanceBaseInfo newFinanceAnalysis = new FinanceBaseInfo(code, Integer.valueOf(date.replaceAll("-", "")));
			try {
				Double yyzsrtbzz = data.getDouble("yyzsrtbzz"); // 营业总收入同比增长(%)
				newFinanceAnalysis.setYyzsrtbzz(yyzsrtbzz);
			} catch (Exception e) {
			}
			try {
				Double gsjlrtbzz = data.getDouble("gsjlrtbzz"); // 归属净利润同比增长(%)
				newFinanceAnalysis.setGsjlrtbzz(gsjlrtbzz);
			} catch (Exception e) {
			}
			try {
				Double kfjlrtbzz = data.getDouble("kfjlrtbzz"); // 扣非净利润同比增长(%)
				newFinanceAnalysis.setKfjlrtbzz(kfjlrtbzz);
			} catch (Exception e) {
			}

			try {
				Long yyzsr = CurrencyUitl.covertToLong(data.get("yyzsr").toString()); // 营业总收入
				newFinanceAnalysis.setYyzsr(yyzsr);
			} catch (Exception e) {
			}
			try {
				Long gsjlr = CurrencyUitl.covertToLong(data.get("gsjlr").toString()); // 归属净利润
				newFinanceAnalysis.setGsjlr(gsjlr);
			} catch (Exception e) {
			}
			try {
				Long kfjlr = CurrencyUitl.covertToLong(data.get("kfjlr").toString()); // 扣非净利润同比增长(%)
				newFinanceAnalysis.setKfjlr(kfjlr);
			} catch (Exception e) {
			}
			list.add(newFinanceAnalysis);
		}
		return list;
	}

	// http://data.eastmoney.com/bbsj/202003/yjyg.html
	// http://data.eastmoney.com/bbsj/yjyg/000662.html
	// private final String yjygBase =
	// "http://data.eastmoney.com/bbsj/yjyg/%s.html";

	// 当年xx03,xx06,xx09,xx12
	// 6月30日之前，xx-1-12
	public List<FinYjkb> getFinYjkb() {
		List<FinYjkb> list2 = new LinkedList<FinYjkb>();
		int currYear = DateUtil.getCurYYYY();
		int currJidu = DateUtil.getCurJidu();

		String date1 = currYear + "-03-31";
		String date2 = currYear + "-06-30";
		String date3 = currYear + "-09-30";
		String date4 = currYear + "-12-31";

		// 快报：准确的
		getYjkbByPage(date1, list2);
		getYjkbByPage(date2, list2);
		getYjkbByPage(date3, list2);
		getYjkbByPage(date4, list2);

		if (currJidu <= 2) {
			int prevYear = currYear - 1;
			String date5 = prevYear + "-12-31";
			getYjkbByPage(date5, list2);
		}
		return list2;
	}

	public List<FinYjyg> getFinYjyg() {
		List<FinYjyg> list = new LinkedList<FinYjyg>();
		int currYear = DateUtil.getCurYYYY();
		int currJidu = DateUtil.getCurJidu();

		String date1 = currYear + "-03-31";
		String date2 = currYear + "-06-30";
		String date3 = currYear + "-09-30";
		String date4 = currYear + "-12-31";

		// 预告：未经审计（类似天气预告，不太准确）
		getYjygByPage(date1, list);
		getYjygByPage(date2, list);
		getYjygByPage(date3, list);
		getYjygByPage(date4, list);

		if (currJidu <= 2) {
			int prevYear = currYear - 1;
			String date5 = prevYear + "-12-31";
			getYjygByPage(date5, list);
		}
		return list;
	}

	private void getYjkbByPage(String date1, List<FinYjkb> list) {
		ThreadsUtil.sleepRandomSecBetween1And5();
		String last = redisUtil.get(RedisConstant.RDS_FIN_KUAIBAO_ + date1);
		last = last == null ? "" : last;
		String lastFromPage = null;
		boolean chkIndor = true;

		String url1 = getYjkbUrl(date1);
		String result = HttpUtil.doGet2(url1);
		result = result.substring("var BEzQbtii=".length());
		result = result.substring(0, result.length() - 1);
		JSONObject objects = JSON.parseObject(result);

		if (objects.getBooleanValue("success")) {
			JSONArray datas = objects.getJSONObject("result").getJSONArray("data");
			for (int i = 0; i < datas.size(); i++) {
				JSONObject data = datas.getJSONObject(i);
				String date = data.getString("REPORT_DATE"); // 报告期
				String anndate = data.getString("NOTICE_DATE"); // 公告日期
				FinYjkb fy = new FinYjkb();
				String datestr = DateUtil.formatYYYYMMDD(DateUtil.parseDate(date, DateUtil.YYYY_MM_DD_HH_MM_SS));
				int y = Integer.valueOf(datestr.substring(0, 4));
				int m = Integer.valueOf(datestr.substring(4, 6));
				int quarter = 0;
				if (m == 12) {
					quarter = 4;
				} else if (m == 9) {
					quarter = 3;
				} else if (m == 6) {
					quarter = 2;
				} else if (m == 3) {
					quarter = 1;
				}
				fy.setYear(y);
				fy.setQuarter(quarter);
				fy.setCode(data.getString("SECURITY_CODE"));
				fy.setDate(Integer
						.parseInt(DateUtil.formatYYYYMMDD(DateUtil.parseDate(anndate, DateUtil.YYYY_MM_DD_HH_MM_SS))));

				try {
					fy.setJlr(data.getLong("PARENT_NETPROFIT"));// 业绩
				} catch (Exception e) {
				}
				try {
					fy.setJlrtbzz(CurrencyUitl.roundHalfUp(data.getDoubleValue("JLRTBZCL")));// 业绩增长
				} catch (Exception e) {
				}
				try {
					fy.setYyzsr(data.getLong("TOTAL_OPERATE_INCOME"));// 营收
				} catch (Exception e) {
				}
				try {
					fy.setYyzsrtbzz(CurrencyUitl.roundHalfUp(data.getDoubleValue("YSTZ")));// 营收幅度
				} catch (Exception e) {
				}
				fy.setId();

				if (chkIndor) {
					if (last.equals(fy.getCode())) {
						chkIndor = false;
					} else {
						list.add(fy);
					}
				}
				if (lastFromPage == null) {
					lastFromPage = fy.getCode();
				}
			}
		}
		// 设置最新
		if (lastFromPage != null) {
			redisUtil.set(RedisConstant.RDS_FIN_KUAIBAO_ + date1, lastFromPage);
		}
	}

	private String getYjkbUrl(String date) {
		return "http://datacenter.eastmoney.com/api/data/get?type=RPT_FCI_PERFORMANCEE&sty=ALL&p=1&ps=50&st=UPDATE_DATE,SECURITY_CODE&sr=-1,-1&var=BEzQbtii&filter=(REPORT_DATE=%27"
				+ date + "%27)&rt=" + System.currentTimeMillis();
	}

	private void getYjygByPage(String date1, List<FinYjyg> list) {
		ThreadsUtil.sleepRandomSecBetween1And5();
		String last = redisUtil.get(RedisConstant.RDS_FIN_YUGAO_ + date1);
		last = last == null ? "" : last;
		String lastFromPage = null;
		boolean chkIndor = true;

		String url1 = getYjygUrl(date1);
		String result = HttpUtil.doGet2(url1);
		result = result.substring("var MRtZkjmw=".length());
		result = result.substring(0, result.length() - 1);
		JSONObject objects = JSON.parseObject(result);
		if (objects.getBooleanValue("success")) {
			JSONArray datas = objects.getJSONObject("result").getJSONArray("data");
			for (int i = 0; i < datas.size(); i++) {
				JSONObject data = datas.getJSONObject(i);
				String date = data.getString("REPORTDATE"); // 报告期
				String anndate = data.getString("NOTICE_DATE"); // 公告日期
				FinYjyg fy = new FinYjyg();
				String datestr = DateUtil.formatYYYYMMDD(DateUtil.parseDate(date, DateUtil.YYYY_MM_DD_HH_MM_SS));
				int y = Integer.valueOf(datestr.substring(0, 4));
				int m = Integer.valueOf(datestr.substring(4, 6));
				int quarter = 0;
				if (m == 12) {
					quarter = 4;
				} else if (m == 9) {
					quarter = 3;
				} else if (m == 6) {
					quarter = 2;
				} else if (m == 3) {
					quarter = 1;
				}
				fy.setYear(y);
				fy.setQuarter(quarter);
				fy.setCode(data.getString("SECURITY_CODE"));
				fy.setDate(Integer
						.parseInt(DateUtil.formatYYYYMMDD(DateUtil.parseDate(anndate, DateUtil.YYYY_MM_DD_HH_MM_SS))));
				try {
					fy.setType(data.getString("FORECASTTYPE"));// 类型
				} catch (Exception e) {
				}
				try {
					fy.setJlr(data.getLong("FORECASTL"));// 业绩范围：FORECASTL-FORECASTT
				} catch (Exception e) {
				}
				try {
					fy.setJlrtbzz(data.getDoubleValue("INCREASET"));// 业绩变动幅度 INCREASET-FORECASTCONTENT
				} catch (Exception e) {
				}
				fy.setId();
				if (chkIndor) {
					if (last.equals(fy.getCode())) {
						chkIndor = false;
					} else {
						list.add(fy);
					}
				}
				if (lastFromPage == null) {
					lastFromPage = fy.getCode();
				}
			}
		}
		// 设置最新
		if (lastFromPage != null) {
			redisUtil.set(RedisConstant.RDS_FIN_YUGAO_ + date1, lastFromPage);
		}
	}

	private String getYjygUrl(String date) {
		return "http://datacenter.eastmoney.com/api/data/get?type=RPT_PUBLIC_OP_PREDICT&sty=ALL&p=1&ps=50&st=NOTICE_DATE,SECURITY_CODE&sr=-1,-1&var=MRtZkjmw&filter=(REPORTDATE=%27"
				+ date + "%27)(IsLatest=%22T%22)&rt=" + System.currentTimeMillis();
	}

	public static void main(String[] args) {
//		EastmoneySpider.getNewFinanceAnalysis("000002", 0);
//		String result = HttpUtil.doGet2(yjygBase);
	}
}
