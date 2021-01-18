package com.stable.spider.ths;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.stable.es.dao.base.FenHongDao;
import com.stable.es.dao.base.ZengFaDao;
import com.stable.es.dao.base.ZengFaDetailDao;
import com.stable.es.dao.base.ZengFaSummaryDao;
import com.stable.service.StockBasicService;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaSummary;

import lombok.extern.log4j.Log4j2;

/**
 * 分红&增发
 */
@Component
@Log4j2
public class ThsBonusSpider {
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;
	private String urlbase = "http://basic.10jqka.com.cn/%s/bonus.html?t=%s";
	private String host = "http://basic.10jqka.com.cn/";
	private Map<String, String> header;

	@Autowired
	private FenHongDao fenHongDao;
	@Autowired
	private ZengFaDao zengFaDao;
	@Autowired
	private ZengFaDetailDao zengFaDetailDao;
	@Autowired
	private ZengFaSummaryDao zengFaSummaryDao;

//1、先由董事会作出决议：方案
//2、提请股东大会批准
//3、由保荐人保荐：申请，向中国证监会申报，保荐人应当按照中国证监会的有关规定编制和报送发行申请文件。
//4、审核
//5、上市公司发行股票：自中国证监会核准发行之日起，上市公司应在6个月内发行股票；超过6个月未发行的，核准文件失效，须重新经中国证监会核准后方可发行。
//6、销售上市公司发行股票

//	<th>增发股票的程序：</th>
//	<tr><td>1、先由董事会作出决议，提出发行方案；</td>	</tr>
//	<tr><td>2、提请股东大会批准；</td></tr>
//	<tr><td>3、由保荐人保荐，并向中国证监会申报；</td>	</tr>
//	<tr><td>4、由发行审核委员会审核，报证监会审核；</td></tr>
//	<tr><td>5、上市公司自证监会审核之日起6个月内发行股票。</td></tr>

	public void dofetchBonus() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					dofetchInner();
				} catch (Exception e) {
					e.printStackTrace();
					ErrorLogFileUitl.writeError(e, "同花顺分红&增发异常运行异常..", "", "");
					WxPushUtil.pushSystem1("同花顺分红&增发异常运行异常");
				}
			}
		}).start();
	}

	private void dofetchInner() {
		if (header == null) {
			header = new HashMap<String, String>();
		}
		int date = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
		List<ZengFaDetail> zfdl = new LinkedList<ZengFaDetail>();
		List<ZengFaSummary> zfsl = new LinkedList<ZengFaSummary>();
		List<FenHong> fhl = new LinkedList<FenHong>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		for (StockBaseInfo s : codelist) {
			try {
				dofetchBonusInner(date, s.getCode(), zfdl, zfsl, fhl);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		if (zfdl.size() > 0) {
			zengFaDetailDao.saveAll(zfdl);
		}
		if (zfsl.size() > 0) {
			zengFaSummaryDao.saveAll(zfsl);
		}
		if (fhl.size() > 0) {
			fenHongDao.saveAll(fhl);
		}
		WxPushUtil.pushSystem1(date + " 分红&增发抓包同花顺已完成");
	}

	private void dofetchBonusInner(int sysdate, String code, List<ZengFaDetail> zfdl, List<ZengFaSummary> zfsl,
			List<FenHong> fhl) {
		int trytime = 0;
		boolean fetched = false;
		String url = String.format(urlbase, code, System.currentTimeMillis());
		ThreadsUtil.sleepRandomSecBetween1And5();
		do {
			try {
				log.info(url);
				header.put("Referer", host + code + "/");
				HtmlPage page = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url, header);
				HtmlElement body = page.getBody();
				// System.err.println(body.asText());
				HtmlElement additionprofile_bd = body.getElementsByAttribute("div", "id", "additionprofile_bd").get(0);
				DomElement sum = additionprofile_bd.getFirstElementChild();
				String sumstr = sum.asText().replaceAll(" ", "");
				ZengFaSummary zfs = new ZengFaSummary();
				zfs.setCode(code);
				zfs.setDesc(sumstr);
				zfs.setUpdate(sysdate);
				zfsl.add(zfs);
//				System.err.println("==================");
//				System.err.println(zfs.toString());
				Iterator<DomElement> it9 = sum.getChildElements().iterator();
				int times = Integer.valueOf(it9.next().asText());
//				System.err.println("==================>" + times);
				if (times > 0) {
					List<HtmlElement> hists = body.getElementsByAttribute("table", "class", "m_table pggk mt10");
					boolean getDetail = false;
					for (HtmlElement h : hists) {
						ZengFa zf = new ZengFa();
						zf.setCode(code);
						zf.setStatus(1);
						Iterator<DomElement> it0 = h.getChildElements().iterator();
						String title = it0.next().asText();
						title = title.replaceAll("方案进度：", ",").replaceAll("发行类型：", ",").replaceAll("发行方式：", ",")
								.replace(" ", "");
						String[] ts = title.split(",");
						zf.setStatusDesc(ts[1]);
						zf.setIssueClz(ts[2]);
						zf.setIssueType(ts[3]);
						if (zf.getStatusDesc().contains("实施")) {
							zf.setStatus(2);
							getDetail = true;
						}
//						实际发行价格：17.0200元 新股上市公告日：2017-03-22
//						实际发行数量：1.97亿股 发行新股日：2017-03-22
//						实际募资净额：33.27亿元 证监会核准公告日：2017-01-18
//						预案发行价格： 17.0200 元 发审委公告日：2016-10-24
//						预案发行数量：不超过2.23亿股 股东大会公告日： 2016-06-08
//						预案募资金额： 38 亿元 董事会公告日：2016-05-17
						DomElement tbody = it0.next();
						Iterator<DomElement> tr = tbody.getChildElements().iterator();
						DomElement tr1 = tr.next();
						try {
							String s1 = tr1.getFirstElementChild().asText().replaceAll("元", "").replaceAll(" ", "")
									.split("：")[1];
							zf.setPrice(Double.valueOf(s1));
						} catch (Exception e) {
						}
						try {
							String s2 = tr1.getLastElementChild().asText().replaceAll(" ", "").split("：")[1];
							zf.setEndDate(DateUtil.convertDate2(s2));
						} catch (Exception e) {
						}
						DomElement tr2 = tr.next();

						try {
							zf.setNum(tr2.getFirstElementChild().asText().replaceAll(" ", "").split("：")[1]);
						} catch (Exception e) {
						}
						DomElement tr3 = tr.next();
						try {
							zf.setAmt(tr3.getFirstElementChild().asText().replaceAll(" ", "").split("：")[1]);
						} catch (Exception e) {
						}
						try {
							String s4 = tr3.getLastElementChild().asText().replaceAll(" ", "").split("：")[1];
							zf.setZjhDate(DateUtil.convertDate2(s4));
						} catch (Exception e) {
							// e.printStackTrace();
						}
						tr.next();
						tr.next();
						DomElement tr6 = tr.next();
						String s5 = tr6.getLastElementChild().asText().replaceAll(" ", "").split("：")[1];
						zf.setStartDate(DateUtil.convertDate2(s5));
						zf.setId(zf.getCode() + zf.getStartDate());
						zf.setUpdate(sysdate);
//						System.err.println("==================");
//						System.err.println(zf.toString());
						zengFaDao.save(zf);
					}
					if (getDetail) {
						HtmlElement detail = body.getElementsByAttribute("table", "class", "m_table m_hl").get(0);// 最近一次明细
						ZengFaDetail zfd = new ZengFaDetail();
						zfd.setCode(code);
						zfd.setDetails(detail.asText());
						String d1 = detail.getFirstElementChild().getFirstElementChild().asText().replaceAll(" ", "")
								.split("：")[1];
						zfd.setDate(DateUtil.convertDate2(d1));
						zfd.setId(zfd.getCode() + zfd.getDate());
						zfd.setUpdate(sysdate);
//						System.err.println("==================");
//						System.err.println(zfd.toString());
						zfdl.add(zfd);
					}

				}

				HtmlElement bonuslist = body.getElementsByAttribute("div", "id", "bonuslist").get(0);
				Iterator<DomElement> it0 = bonuslist.getChildElements().iterator();
				it0.next();// <h2>分红情况</h2>
				DomElement de0 = it0.next().getFirstElementChild();
				String fhstr = de0.asText().replaceAll(" ", "");
//				System.err.println("==================");
				FenHong fh = new FenHong();
				fh.setCode(code);
				fh.setDetails(fhstr);
//				System.err.println(fhstr);
				Iterator<DomElement> it1 = de0.getChildElements().iterator();
				fh.setTimes(Integer.valueOf(it1.next().asText()));
				fh.setPrice(Double.valueOf(it1.next().asText()));
				fh.setUpdate(sysdate);
//				System.err.println(fh.toString());
				fhl.add(fh);
//				if(times2>0) {
//					HtmlElement bonuslist = body.getElementsByAttribute("div", "id", "bonus_table").get(0);
//				}
				return;

			} catch (Exception e2) {
				e2.printStackTrace();
				trytime++;
				ThreadsUtil.sleepRandomSecBetween15And30(trytime);
				if (trytime >= 10) {
					fetched = true;
					e2.printStackTrace();
					WxPushUtil.pushSystem1("同花顺-分红&增发获取出错,url=" + url);
				}
			} finally {
				htmlunitSpider.close();
			}
		} while (!fetched);
	}

	public static void main(String[] args) {
		ThsBonusSpider ts = new ThsBonusSpider();
		ts.htmlunitSpider = new HtmlunitSpider();
		ts.header = new HashMap<String, String>();
		List<ZengFaDetail> zfdl = new LinkedList<ZengFaDetail>();
		List<ZengFaSummary> zfsl = new LinkedList<ZengFaSummary>();
		List<FenHong> fhl = new LinkedList<FenHong>();
		ts.dofetchBonusInner(DateUtil.getTodayIntYYYYMMDD(), "002405", zfdl, zfsl, fhl);
	}
}
