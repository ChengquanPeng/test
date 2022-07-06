package com.stable.spider.eastmoney;

import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.msg.MsgPushServer;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.ZhiYa;
import com.stable.vo.bus.ZhiYaDetail;

import lombok.extern.log4j.Log4j2;

/*
 * 质押-东方财富
 * 
 * https://data.eastmoney.com/gpzy/detail/600617.html
 * 
 */

@Component
@Log4j2
public class EastmoneyZytjSpider2 {

	private String FIXIED = "jQuery112309870185672726921_1628497268805";
	private String START = "http://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112309870185672726921_1628497268805&sortColumns=NOTICE_DATE&sortTypes=-1&pageSize=50&pageNumber=1&reportName=RPTA_APP_ACCUMDETAILS&columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22";;
	private String END = "%22)&t=zymx";

	public synchronized List<ZhiYaDetail> getZy(String code) {
		int trytime = 0;
		boolean fetched = false;
		String url = START + code + END;
		do {
			try {
				ThreadsUtil.sleepRandomSecBetween1And2();
				String result = HttpUtil.doGet2(url);
				log.info(result);
				result = result.substring(FIXIED.length() + 1, result.length() - 2);
				log.info(result);
				JSONObject object = JSON.parseObject(result);
				if (object == null || !object.getBooleanValue("success")) {
					// 未ok、
					log.info("no data OK=" + code);
					return null;
				}
				JSONArray objects = object.getJSONObject("result").getJSONArray("data");
				if (objects == null || objects.size() <= 0) {
					// 未ok、
					log.info("Not OK=" + code);
					return null;
				}
				fetched = true;

				List<ZhiYaDetail> l = new LinkedList<ZhiYaDetail>();
				for (int k = 0; k < objects.size(); k++) {
					JSONObject data = objects.getJSONObject(k);
					ZhiYaDetail zyd = new ZhiYaDetail();
					zyd.setCode(code);
					String UNFREEZE_STATE = data.getString("UNFREEZE_STATE");
					zyd.setStateDesc(UNFREEZE_STATE);
					// 状态-1已解压,2,未达预警线,3已达预警线，4.其他
					if (UNFREEZE_STATE.startsWith("已解")) {
						zyd.setState(1);
					} else if (UNFREEZE_STATE.startsWith("未")) {
						zyd.setState(2);
					} else if (UNFREEZE_STATE.startsWith("预警线")) {
						zyd.setState(3);
					} else {
						zyd.setState(4);
					}
					zyd.setHolderName(getStr(data.getString("HOLDER_NAME")));// 质押股东
					zyd.setPurpose(getStr(data.getString("PF_PURPOSE")));// 质押目的
					zyd.setNum(data.getLong("PF_NUM"));// 质押股份数量
					try {
						zyd.setSelfRatio(data.getDouble("PF_HOLD_RATIO"));// 占所持比例
					} catch (Exception e) {
					}
					try {
						zyd.setTotalRatio(data.getDouble("PF_TSR"));// 占总股本比例
					} catch (Exception e) {
					}
					zyd.setClosePrice(data.getDouble("CLOSE_FORWARD_ADJPRICE"));// 质押日收盘价(元)
					try {
						zyd.setOpenline(data.getDouble("OPENLINE"));// 预估平仓线
					} catch (Exception e) {
					}
					try {
						zyd.setWarningLine(data.getDouble("WARNING_LINE"));// 预警线
					} catch (Exception e) {
					}
					zyd.setNoticeDate(DateUtil.getDateStrToIntYYYYMMDDHHMMSS(data.getString("NOTICE_DATE")));// 公告日
					zyd.setStartDate(DateUtil.getDateStrToIntYYYYMMDDHHMMSS(data.getString("PF_START_DATE")));// 开始日期
					try {
						zyd.setUnfreezeDate(
								DateUtil.getDateStrToIntYYYYMMDDHHMMSS(data.getString("ACTUAL_UNFREEZE_DATE")));// 结束日期
					} catch (Exception e) {
					}
					zyd.setPfOrg(getStr(data.getString("PF_ORG")));
					String id = code + "_" + zyd.getStartDate() + "_" + zyd.getNum() + "_"
							+ zyd.getHolderName().hashCode() + "_" + zyd.getPfOrg().hashCode();
					zyd.setId(id);
					l.add(zyd);
				}
				return l;
			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					fetched = true;
					e2.printStackTrace();
					MsgPushServer.pushSystem1("东方财富-股东质押2获取出错,url=" + url);
				}
			} finally {

			}
		} while (!fetched);
		return null;
	}

	public String getStr(String s) {
		if (s == null) {
			return "0";
		}
		return s.trim();
	}

	public static void main(String[] args) {
		EastmoneyZytjSpider2 ts = new EastmoneyZytjSpider2();
		System.err.println(ts.getZyT("603206"));
	}

	private String FIXIED_T = "jQuery1123015124691219584796_1651937007370";
	private String START_T = "https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery1123015124691219584796_1651937007370&sortColumns=TRADE_DATE&sortTypes=-1&pageSize=5&pageNumber=1&reportName=RPT_CSDC_LIST&columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22";
	private String END_T = "%22)";

	public synchronized ZhiYa getZyT(String code) {
		int trytime = 0;
		boolean fetched = false;
		String url = START_T + code + END_T;
		do {
			try {
				ThreadsUtil.sleepRandomSecBetween1And2();
//				log.info(url);
//				System.err.println(url);
				String result = HttpUtil.doGet2(url);
//				log.info(result);
				result = result.substring(FIXIED_T.length() + 1, result.length() - 2);
//				log.info(result);
				JSONObject object = JSON.parseObject(result);
				if (object == null || !object.getBooleanValue("success")) {
					// 未ok、
					log.info("no data2 =" + code);
					return new ZhiYa();
				}
				JSONArray objects = object.getJSONObject("result").getJSONArray("data");
				if (objects == null || objects.size() <= 0) {
					// 未ok、
					log.info("Not OK2 =" + code);
					return new ZhiYa();
				}
				fetched = true;

				ZhiYa zy = new ZhiYa();
				if (objects.size() > 0) {
					JSONObject data = objects.getJSONObject(0);
					zy.setTotalRatio(data.getDouble("PLEDGE_RATIO"));
					return zy;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					fetched = true;
					e2.printStackTrace();
					MsgPushServer.pushSystem1("东方财富-股东质押T获取出错,url=" + url);
				}
			} finally {

			}
		} while (!fetched);
		return new ZhiYa();
	}

}
