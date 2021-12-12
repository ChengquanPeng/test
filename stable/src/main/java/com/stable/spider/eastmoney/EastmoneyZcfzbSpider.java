package com.stable.spider.eastmoney;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.Constant;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.FinanceZcfzb;

//**
//资产负债表
//**
@Component
public class EastmoneyZcfzbSpider {

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

	/**
	 * 财务信息
	 * 
	 * @param code 6位普通股票代码
	 * @param type 0按报告期、1=年报
	 *             http://f10.eastmoney.com/NewFinanceAnalysis/zcfzbAjax?companyType=4&reportDateType=0&reportType=2&endDate=&code=SZ002752
	 */
	public static final String financeUrl = "http://f10.eastmoney.com/NewFinanceAnalysis/zcfzbAjaxNew?companyType=4&reportDateType=0&reportType=1&dates=%s&code=%s";
	public static final String fud = "http://f10.eastmoney.com/NewFinanceAnalysis/lrbDateAjaxNew?companyType=4&reportDateType=0&code=%s";

	public static String getDates(String code) {
		int trytime = 0;
		do {
			trytime++;
			try {

				ThreadsUtil.sleepRandomSecBetween1And2();
				String url = String.format(fud, formatCode2(code), System.currentTimeMillis());
				System.err.println(url);
				String result = HttpUtil.doGet2(url);
				System.err.println(result);
				JSONObject jsonobj = JSON.parseObject(result);
				JSONArray objects = jsonobj.getJSONArray("data");

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < objects.size(); i++) {
					JSONObject data = objects.getJSONObject(i);
					FinanceZcfzb fzb = new FinanceZcfzb();
					fzb.setCode(code);
					String date = data.get("REPORT_DATE").toString(); // 年报日期
					date = date.substring(0, 10);
					sb.append(date);
					if (i >= 5) {
						break;
					}
					sb.append(Constant.DOU_HAO);
				}
				return sb.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			ThreadsUtil.sleepRandomSecBetween15And30(trytime);
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-财务(资产负债表-dates)-抓包出错,code=" + code);
		return null;
	}

	public static Map<String, FinanceZcfzb> getZcfzb(String code, String dates) {
		int trytime = 0;
		do {
			trytime++;
			try {
				ThreadsUtil.sleepRandomSecBetween1And2();
				Map<String, FinanceZcfzb> m = new HashMap<String, FinanceZcfzb>();
				String url = String.format(financeUrl, dates, formatCode2(code), System.currentTimeMillis());
				System.err.println(url);
				String result = HttpUtil.doGet2(url);
				System.err.println(result);
				JSONObject jsonobj = JSON.parseObject(result);
				JSONArray objects = jsonobj.getJSONArray("data");
				for (int i = 0; i < objects.size(); i++) {
					JSONObject data = objects.getJSONObject(i);
					FinanceZcfzb fzb = new FinanceZcfzb();
					fzb.setCode(code);
					String date = data.get("REPORT_DATE").toString(); // 年报日期
					fzb.setDate(
							DateUtil.formatYYYYMMDDReturnInt(DateUtil.parseDate(date, DateUtil.YYYY_MM_DD_HH_MM_SS)));
					fzb.setId(code + "_" + fzb.getDate());

					try {
						fzb.setGoodWill(Double.valueOf(data.getString("GOODWILL")));// 商誉
					} catch (Exception e) {
					}
					try {
						fzb.setSumAsset(Double.valueOf(data.getString("SUMASSET")));// 总资产 x
					} catch (Exception e) {
					}
					try {
						fzb.setSumDebt(Double.valueOf(data.getString("SUMLIAB")));// 负债总计 x
					} catch (Exception e) {
					}
					try {
						fzb.setSumDebtLd(Double.valueOf(data.getString("SUMLLIAB")));// 流动负债总计
					} catch (Exception e) {
					}
					try {
						fzb.setNetAsset(Double.valueOf(data.getString("SUMSHEQUITY")));// 净资产
					} catch (Exception e) {
					}
					try {
						fzb.setInventory(Double.valueOf(data.getString("INVENTORY")));// 存货资产
					} catch (Exception e) {
					}

					try {
						fzb.setAccountrec(Double.valueOf(data.getString("ACCOUNTREC")));// 应收账款（是否自己贴钱在干活，同行业比较）
					} catch (Exception e) {
					}
					try {
						fzb.setInterestPay(Double.valueOf(data.getString("INTERESTPAY")));// 应付利息:如果较高，公司在大量有息借钱，关联到货币资金和未分配利润。如果货币资金和未分配利润较高，明明有钱为什么借钱，
					} catch (Exception e) {
					}
					try {
						fzb.setAccountPay(Double.valueOf(data.getString("ACCOUNTPAY")));// 应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
					} catch (Exception e) {
					}
					try {
						fzb.setRetaineDearning(Double.valueOf(data.getString("RETAINEDEARNING")));// 未分配利润
					} catch (Exception e) {
					}
					try {
						fzb.setSumLasset(Double.valueOf(data.getString("SUMLASSET")));// 流动资产合计
					} catch (Exception e) {
					}
					try {
						fzb.setMonetaryFund(Double.valueOf(data.getString("MONETARYFUND")));// 货币资金
					} catch (Exception e) {
					}
					try {
						fzb.setTradeFinassetNotfvtpl(Double.valueOf(data.getString("TRADE_FINASSET_NOTFVTPL")));// 可交易金融资产
					} catch (Exception e) {
					}

					try {
						fzb.setStborrow(Double.valueOf(data.getString("STBORROW")));// 短期借款-STBORROW
					} catch (Exception e) {
					}
					try {
						fzb.setLtborrow(Double.valueOf(data.getString("LTBORROW")));// 长期借款-LTBORROW
					} catch (Exception e) {
					}
//					System.err.println(fzb + " " + CurrencyUitl.covertToString(fzb.getSumAsset()) + " "
//							+ CurrencyUitl.covertToString(fzb.getSumDebt()) + " "
//							+ CurrencyUitl.covertToString(fzb.getNetAsset()));
					m.put(fzb.getId(), fzb);
				}
				return m;
			} catch (Exception e) {
				e.printStackTrace();
			}
			ThreadsUtil.sleepRandomSecBetween15And30(trytime);
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-财务(资产负债表)-抓包出错,code=" + code);
		return new HashMap<String, FinanceZcfzb>();
	}

	// http://data.eastmoney.com/bbsj/202003/yjyg.html
	// http://data.eastmoney.com/bbsj/yjyg/000662.html
	// private final String yjygBase =
	// "http://data.eastmoney.com/bbsj/yjyg/%s.html";

	public static void main(String[] args) {
		String code = "600446";
		Map<String, FinanceZcfzb> m = EastmoneyZcfzbSpider.getZcfzb(code, EastmoneyZcfzbSpider.getDates(code));
		for (String key : m.keySet()) {
			System.err.println(m.get(key));
		}
//		System.err.println(EastmoneyZcfzbSpider.getDates("002405"));
//		System.err.println(CurrencyUitl.roundHalfUp( 5.9548019532E8/4.56697178601E9));
	}

	/**
	 * 财务信息
	 * 
	 * @param code 6位普通股票代码
	 * @param type 0按报告期、1=年报
	 * @return http://f10.eastmoney.com/NewFinanceAnalysis/MainTargetAjax?type=1&code=SZ300750
	 */
	static final String financeUrlxjl = "http://f10.eastmoney.com/NewFinanceAnalysis/xjllbAjax?companyType=4&reportDateType=%s&reportType=1&endDate=&code=%s";

	// http://f10.eastmoney.com/NewFinanceAnalysis/lrbAjaxNew?companyType=4&reportDateType=0&reportType=1&dates=2021-09-30%2C2021-06-30%2C2021-03-31%2C2020-12-31%2C2020-09-30&code=SH600446
	@SuppressWarnings("deprecation")
	public static Map<String, FinanceZcfzb> getXjllb(String code, String dates) {
		int trytime = 0;
		do {
			trytime++;
			try {
				ThreadsUtil.sleepRandomSecBetween1And2();
				Map<String, FinanceZcfzb> m = new HashMap<String, FinanceZcfzb>();
				String url = String.format(financeUrlxjl, formatCode2(code), System.currentTimeMillis());
				String result = HttpUtil.doGet2(url);
				result = result.substring(1, result.length() - 1);
				result = StringUtils.replaceAll(result, "\\\\", "");
				JSONArray objects = JSON.parseArray(result);
				for (int i = 0; i < objects.size(); i++) {
					JSONObject data = objects.getJSONObject(i);
					FinanceZcfzb fzb = new FinanceZcfzb();
					fzb.setCode(code);
					String date = data.get("REPORTDATE").toString(); // 年报日期
					fzb.setDate(DateUtil.formatYYYYMMDDReturnInt(DateUtil.parseDate3(date)));
					fzb.setId(code + "_" + fzb.getDate());

					try {
						fzb.setGoodWill(Double.valueOf(data.getString("NETOPERATECASHFLOW")));// 商誉
					} catch (Exception e) {
					}
//					System.err.println(fzb + " " + CurrencyUitl.covertToString(fzb.getSumAsset()) + " "
//							+ CurrencyUitl.covertToString(fzb.getSumDebt()) + " "
//							+ CurrencyUitl.covertToString(fzb.getNetAsset()));
					m.put(fzb.getId(), fzb);
				}
				return m;
			} catch (Exception e) {
				e.printStackTrace();
			}
			ThreadsUtil.sleepRandomSecBetween15And30(trytime);
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("东方财富-财务(现金流量表)-抓包出错,code=" + code);
		return new HashMap<String, FinanceZcfzb>();
	}
}
