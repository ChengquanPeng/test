package com.stable.spider.eastmoney;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.AddIssueDao;
import com.stable.service.StockBasicService;
import com.stable.utils.AddIssueUtil;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.AddIssue;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EmAddIssueSpider {

//	1、先由董事会作出决议：方案
//	2、提请股东大会批准
//	3、由保荐人保荐：申请，向中国证监会申报，保荐人应当按照中国证监会的有关规定编制和报送发行申请文件。
//	4、审核
//	5、上市公司发行股票：自中国证监会核准发行之日起，上市公司应在6个月内发行股票；超过6个月未发行的，核准文件失效，须重新经中国证监会核准后方可发行。
//	6、销售上市公司发行股票

	private String BASE_URL = "http://guba.eastmoney.com/list,%s,3,f_%s.html";
	@Autowired
	private HtmlunitSpider htmlunitSpider;// = new HtmlunitSpider();
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private AddIssueDao addIssueDao;

	public void dofetch(int fromDate) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					int endDate = fromDate;
					if (fromDate == 0) {
						endDate = Integer.valueOf(DateUtil.formatYYYYMMDD(DateUtil.addDate(new Date(), -500)));
					}
					log.info("fromDate:{},endDate:{}", fromDate, endDate);
					List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
					List<AddIssue> res = new LinkedList<AddIssue>();
					for (StockBaseInfo b : list) {
						AddIssue iss = dofetch(b.getCode(), endDate).getAddIssue();
						if (iss.getStartDate() > 0) {
							res.add(iss);
						}
					}
					if (res.size() > 0) {
						addIssueDao.saveAll(res);
					}
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("东方财富-抓包公告出错-抓包出错");
				}
			}
		}).start();
	}

	private AddIssueUtil dofetch(String code, int endDate) {
		AddIssueUtil util = new AddIssueUtil();
		util.setCode(code);
		for (int i = 1; i < 5; i++) {

			String url = String.format(BASE_URL, code, i);
			int trytime = 0;
			boolean fetched = false;
			do {
				ThreadsUtil.sleepRandomSecBetween1And5();
				HtmlPage page = null;
				HtmlElement body = null;
				try {
					log.info(url);
					page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url);
					body = page.getBody();
					List<HtmlElement> list = body.getElementsByAttribute("div", "class", "articleh normal_post");// list
					String pageEndDateStr = "";
					for (HtmlElement item : list) {
						// 阅读,评论,标题,公告类型,公告日期
						Iterator<DomElement> it = item.getChildElements().iterator();
						it.next();
						it.next();
						String title = it.next().getFirstElementChild().getAttribute("title");
						String type = it.next().asText();
						String date = it.next().asText();

//						System.err.println(date + " " + type + " " + title);
						if (type.contains("增发")) {
							// 成功
							if (type.contains("上市公告书") || title.contains("发行情况报告书")) {
								// System.err.println("999999-endingggg:" + date + " " + type + " " + title);
								util.addLine(2, date, title);
							}
							// 终止
							if (type.contains("终止")) {
								// System.err.println("999999-dieddddd:" + date + " " + type + " " + title);
								util.addLine(3, date, title);
							}
						}
						// 发行
						if (title.contains("发行") && (title.contains("股票") || title.contains("股份"))
								&& title.contains("案")) {
//							System.err.println("999999-startingggg:" + date + " " + type + " " + title);
							if (!util.addLine(1, date, title)) {
								return util;
							}
						}
						pageEndDateStr = date;
					}
					fetched = true;
					int pageEndDate = DateUtil.convertDate2(pageEndDateStr);
					if (pageEndDate < endDate) {
						return util;
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				} finally {
					htmlunitSpider.close();
				}
				trytime++;
				ThreadsUtil.sleepRandomSecBetween1And5(trytime);
				if (trytime >= 3) {
					fetched = true;
					WxPushUtil.pushSystem1("东方财富-抓包公告出错-抓包出错code=" + code + ",url=" + url);
				}
			} while (!fetched);
		}
		return util;
	}

	public static void main(String[] args) {
		EmAddIssueSpider tp = new EmAddIssueSpider();
		tp.htmlunitSpider = new HtmlunitSpider();
		String[] as = { "603385", "300676", "002405", "601369", "600789", "002612" };
		List<AddIssueUtil> res = new LinkedList<AddIssueUtil>();
		for (int i = 0; i < as.length; i++) {
			res.add(tp.dofetch(as[i], 20180101));
		}
		for (AddIssueUtil r : res) {
			System.err.println(r.getAddIssue());
		}
	}

}
