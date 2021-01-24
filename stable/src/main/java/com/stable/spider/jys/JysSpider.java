package com.stable.spider.jys;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.AnnouncementHistDao;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.utils.MathUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.AnnMentParam;
import com.stable.vo.AnnMentParamUtil;
import com.stable.vo.bus.AnnouncementHist;

import lombok.extern.log4j.Log4j2;

/**
 * 交易所（1深圳，2上海）
 */
@Component
@Log4j2
public class JysSpider {
	private String szUrlbase = "http://www.szse.cn/api/disc/announcement/annList?random=%s";

	@Autowired
	private AnnouncementHistDao announcementHistDao;

	private String zaiquan = "债券";

	public void byJob() {
		dofetchInner();
	}

	public void byWeb() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				dofetchInner();
			}
		}).start();
	}

	private synchronized void dofetchInner() {
		try {
			if (header == null) {
				header = new HashMap<String, String>();
				header.put("Referer", "http://www.sse.com.cn/disclosure/listedinfo/announcement/");
			}
			for (AnnMentParam t : AnnMentParamUtil.types) {
				Collection<AnnouncementHist> l1 = szjys(t.getType(), t.getParamCn());
				if (l1.size() > 0) {
					announcementHistDao.saveAll(l1);
				}
				Collection<AnnouncementHist> l2 = shjys(t.getType(), t.getParamGet());
				if (l2.size() > 0) {
					announcementHistDao.saveAll(l2);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "交易所公告:减持，增持，回购", "", "");
			WxPushUtil.pushSystem1("交易所公告:减持，增持，回购");
		}
	}

	int szPageSize = 30;
	private Map<String, String> header;

	/**
	 * 深圳交易所
	 */
	private List<AnnouncementHist> szjys(int type, String param) {
		List<AnnouncementHist> list = new LinkedList<AnnouncementHist>();
		int trytime = 0;
		boolean fetched = false;
		int sysupdate = DateUtil.getTodayIntYYYYMMDD();
		String url = String.format(szUrlbase, System.currentTimeMillis());

		for (int i = 1; i <= 10; i++) {
			do {
				try {
					log.info(url);
					String json = "{\"seDate\":[\"\",\"\"],\"searchKey\":[\"" + param
							+ "\"],\"channelCode\":[\"listedNotice_disc\"],\"pageSize\":" + szPageSize + ",\"pageNum\":"
							+ i + "}";
					System.err.println(json);
					String r = HttpUtil.doPost2(url, json);
//					System.err.println(r);
					JSONObject jsonObj = JSON.parseObject(r);
					JSONArray arr = jsonObj.getJSONArray("data");
					if (arr.size() > 0) {
						for (int j = 0; j < arr.size(); j++) {
							JSONObject d = arr.getJSONObject(j);
							String title = d.getString("title");
							if (!title.contains(zaiquan)) {
								AnnouncementHist ah = new AnnouncementHist();
								ah.setSoureId(d.getString("id"));
								ah.setCode(d.getJSONArray("secCode").getString(0));
								ah.setId(ah.getCode() + ah.getSoureId());
								ah.setRptDate(DateUtil.getTodayYYYYMMDDHHMMSS(d.getString("publishTime")));
								ah.setTitle(title);
								ah.setUpdate(sysupdate);
								ah.setType(type);
								ah.setMkt(1);
//								ah.setUrl(d.getString("attachPath"));
								log.info(ah);
								list.add(ah);
							}
						}
						fetched = true;
					}
					ThreadsUtil.sleepRandomSecBetween15And30();
				} catch (Exception e2) {
					e2.printStackTrace();
					trytime++;
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					if (trytime >= 10) {
						fetched = true;
						e2.printStackTrace();
						WxPushUtil.pushSystem1("深圳交易所公告-减持，增持，回购，获取出错,url=" + url);
					}
				} finally {

				}
			} while (!fetched);
		}
		return list;
	}

	// 上海
	private String securityType = "0101%2C120100%2C020100%2C020200%2C120200";

//	参数意义	securityType
//	全部	0101,120100,020100,020200,120200
//	主板	0101
//	科创板	120100,020100,020200,120200
//	isPagination这名字一看就应该是是否分页，为了方便爬取，我们将该参数设为false
//	productId股票代码
//	keyWord关键字
//	securityType证券类型
	private Set<AnnouncementHist> shjys(int type, String param) {
		Set<AnnouncementHist> list = new HashSet<AnnouncementHist>();
		int trytime = 0;
		boolean fetched = false;
		Date today = new Date();
		String startDate = DateUtil.formatYYYYMMDD2(DateUtil.addDate(today, -365));
		String endDate = DateUtil.formatYYYYMMDD2(today);
		int sysupdate = DateUtil.getTodayIntYYYYMMDD();
		String url = "";
		for (int i = 1; i <= 10; i++) {
			do {
				try {
					String starturl = "jsonpCallback" + MathUtil.getRandom4bit();
					url = "http://query.sse.com.cn/security/stock/queryCompanyBulletin.do?" + starturl
							+ "&isPagination=true&productId=&keyWord=" + param + "&securityType=" + securityType
							+ "&reportType2=&reportType=ALL&beginDate=" + startDate + "&endDate=" + endDate
							+ "&pageHelp.pageNo=" + i //
							+ "&pageHelp.beginPage=" + i//
							+ "&pageHelp.pageSize=25&pageHelp.pageCount=50"//
//							+ "&pageHelp.cacheSize=1&pageHelp.endPage=5" //
							+ "&_=" + System.currentTimeMillis();
//					System.err.println(url);
					String r = HttpUtil.doGet3_1(url, header);
//					System.err.println("1:" + r);
//					r = r.substring(starturl.length(), r.length() - 1);
//					System.err.println("2:" + r);
					JSONObject jsonObj = JSON.parseObject(r);
					JSONArray arr = jsonObj.getJSONArray("result");
					if (arr.size() > 0) {
						for (int j = 0; j < arr.size(); j++) {
							JSONObject d = arr.getJSONObject(j);
							String title = d.getString("TITLE");
							if (!title.contains(zaiquan)) {
								AnnouncementHist ah = new AnnouncementHist();
//								ah.setSoureId(d.getString("SECURITY_CODE"));
								ah.setCode(d.getString("SECURITY_CODE"));
								ah.setMkt(2);
								ah.setRptDate(DateUtil.convertDate2(d.getString("SSEDATE")));
								ah.setTitle(title);
								ah.setUpdate(sysupdate);
								ah.setType(type);
								ah.setUrl(d.getString("URL"));
								ah.setId(ah.getCode() + ah.getRptDate() + ah.getTitle().hashCode());// TODO
								log.info(ah);
								list.add(ah);
							}
						}
						fetched = true;
					}
					ThreadsUtil.sleepRandomSecBetween15And30();
//					return;
				} catch (Exception e2) {
					e2.printStackTrace();
					trytime++;
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					if (trytime >= 10) {
						fetched = true;
						e2.printStackTrace();
						WxPushUtil.pushSystem1("深圳交易所公告-减持，增持，回购，获取出错,url=" + url);
					}
				} finally {

				}
			} while (!fetched);
		}
		return list;
	}

	public static void main(String[] args) {
		JysSpider ts = new JysSpider();
		ts.header = new HashMap<String, String>();
		ts.header.put("Referer", "http://www.sse.com.cn/disclosure/listedinfo/announcement/");
		Collection<AnnouncementHist> list = ts.shjys(1, "%E5%A2%9E%E6%8C%81");
		for (AnnouncementHist ah : list) {
			log.info(ah);
		}
	}
}
