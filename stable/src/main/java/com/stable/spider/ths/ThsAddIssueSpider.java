package com.stable.spider.ths;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.AddIssueDao;
import com.stable.service.ChipsService;
import com.stable.service.StockBasicService;
import com.stable.utils.AddIssueUtil;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.UnicodeUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.AddIssue;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ThsAddIssueSpider {

//	1、先由董事会作出决议：方案
//	2、提请股东大会批准
//	3、由保荐人保荐：申请，向中国证监会申报，保荐人应当按照中国证监会的有关规定编制和报送发行申请文件。
//	4、审核
//	5、上市公司发行股票：自中国证监会核准发行之日起，上市公司应在6个月内发行股票；超过6个月未发行的，核准文件失效，须重新经中国证监会核准后方可发行。
//	6、销售上市公司发行股票

	private String BASE_URL = "http://basic.10jqka.com.cn/api/stock/getsnlist/%s_%s.json";
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private AddIssueDao addIssueDao;
	@Autowired
	private ChipsService chipsService;
	private ReentrantLock lock = new ReentrantLock();

	public void dofetch(boolean isJob, int fromDate) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (lock.tryLock(12, TimeUnit.HOURS)) {
						log.info("getLock");
						try {
							int endDate = fromDate;
							log.info("fromDate:{},endDate:{}", fromDate, endDate);
							List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
							for (StockBaseInfo b : list) {
								AddIssue iss = dofetch(b.getCode(), endDate, isJob).getAddIssue();
								if (iss.getStartDate() > 0) {
									addIssueDao.save(iss);
								}
							}
							log.info("增发完成抓包");
						} catch (Exception e) {
							e.printStackTrace();
							WxPushUtil.pushSystem1("同花顺-抓包公告出错-抓包出错2");
						} finally {
							lock.unlock();
						}
					} else {
						log.info("No Lock");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}).start();

	}

	private AddIssueUtil dofetch(String code, int endDate, boolean isJob) {
		ThreadsUtil.sleepRandomSecBetween1And5();
		boolean fatchOnePage = true;
		// 1.定时任务+（null空或者已完成的）就只抓一页。
		// 2.如果有未完成的则抓取2年的记录（国企的定增时间可能比较慢）
		if (isJob) {
			AddIssue ai = chipsService.getLastAddIssue(code);
			if (ai != null && ai.getStatus() == 1) {
				fatchOnePage = false;
			}
		}
		AddIssueUtil util = new AddIssueUtil();
		util.setCode(code);
		for (int i = 1; i < 30; i++) {// 30页
			String url = String.format(BASE_URL, code, i);
			int trytime = 0;
			boolean fetched = false;
			do {
				try {
					log.info(url);
					ThreadsUtil.sleepRandomSecBetween1And2();
					String result = HttpUtil.doGet2(url);
					result = UnicodeUtil.UnicodeToCN(result);
					JSONArray objects = JSON.parseArray(result);
					String s_date = "";
					for (int j = 0; j < objects.size(); j++) {
						JSONObject data = objects.getJSONObject(j);
						String title = data.getString("title");
						String date = data.getString("date");
						// 成功
						if (title.contains("上市公告书") || title.contains("发行情况报告书")) {
							// System.err.println("999999-endingggg:" + date + " " + type + " " + title);
							util.addLine(2, date, title);
						}
						// 终止
						if (title.contains("终止") && title.contains("发行")) {
							// System.err.println("999999-dieddddd:" + date + " " + type + " " + title);
							util.addLine(3, date, title);
						}
						// 发行
						if (title.contains("发行") && title.contains("案") && title.contains("股")) {
//							System.err.println("999999-startingggg:" + date + " " + type + " " + title);
							if (!util.addLine(1, date, title)) {
								return util;
							}
						}
						s_date = date;
					}
					fetched = true;
					int pageEndDate = 0;
					try {
						pageEndDate = DateUtil.convertDate2(s_date);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (isJob && fatchOnePage) {
						return util;
					}
					if (pageEndDate < endDate) {
						return util;
					}
				} catch (Exception e2) {
					e2.printStackTrace();
					trytime++;
					ThreadsUtil.sleepRandomSecBetween15And30(trytime);
					if (trytime >= 10) {
						fetched = true;
						WxPushUtil.pushSystem1("同花顺-抓包公告出错-抓包出错code=" + code + ",url=" + url);
					}
				} finally {
				}
			} while (!fetched);
		}
		return util;

	}

	public static void main(String[] args) {
		ThsAddIssueSpider tp = new ThsAddIssueSpider();
//		String[] as = { "603385", "300676", "002405", "601369", "600789", "002612" };
		String[] as = { "600789" };
		List<AddIssueUtil> res = new LinkedList<AddIssueUtil>();
		for (int i = 0; i < as.length; i++) {
			res.add(tp.dofetch(as[i], 20160101, false));
		}
		for (AddIssueUtil r : res) {
			System.err.println(r.getAddIssue());
		}
	}

}
