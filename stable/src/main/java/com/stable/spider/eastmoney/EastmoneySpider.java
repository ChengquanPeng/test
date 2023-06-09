package com.stable.spider.eastmoney;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.EsFinYjkbDao;
import com.stable.es.dao.base.EsFinYjygDao;
import com.stable.service.StockBasicService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.HtmlunitSpider;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.FinanceZcfzb;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfoPage;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EastmoneySpider {
	@Autowired
	private EsFinYjygDao esFinYjygDao;
	@Autowired
	private EsFinYjkbDao esFinYjkbDao;
	@Autowired
	private HtmlunitSpider htmlunitSpider;
	@Autowired
	private StockBasicService stockBasicService;

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

	/**
	 * 财务信息-主要指标
	 * 
	 * @param code 6位普通股票代码
	 * @param type 0按报告期、1=年报、2=单季度
	 * @return http://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/Index?type=web&code=sz000001
	 */
	static final String financeUrl = "http://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/ZYZBAjaxNew?type=%s&code=%s";
	static final double Yi10 = CurrencyUitl.YI_N.doubleValue() * 10;// 10亿

	// http://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/Index?type=web&code=sz000001
	// 1.主要指标ZYZBAjaxNew 返回9条记录
	// 2.杜邦分析DBFXAjaxNew 返回9条记录
	// 3.百分比分析bfbbbAjaxNew 返回12条记录
	// 4.资产负债表zcfzbAjaxNew 返回5条记录
	// 5.利润表lrbAjaxNew 返回5条记录
	// 6.现金流xjllbAjaxNew 返回5条记录
	static final int FETCH_CNT = 5;// 所以默认返回5条

	// companyType:1券商，2保险,3银行，4企业
	public List<FinanceBaseInfoPage> getNewFinanceAnalysis(String code, int companyType, int beforeChkDate) {
		if (companyType == 0) {
			companyType = getcompanyType(code);
			if (companyType == 0) {
				MsgPushServer.pushToSystem("东方财富-财务companyType-抓包出错,code=" + code);
				return null;
			}
		}

		if (!stockBasicService.onlinePreYearChk(code, beforeChkDate)) {
			log.warn("未抓取到Finane记录,未满半年,code={}", code);
			return null;
		}

		int trytime = 0;
		do {
			trytime++;
			try {
				List<FinanceBaseInfoPage> list = new ArrayList<FinanceBaseInfoPage>();
				String url = String.format(financeUrl, 0, formatCode2(code));
				String result = HttpUtil.doGet2(url);
				JSONObject jsonobj = JSON.parseObject(result);
				JSONArray objects = jsonobj.getJSONArray("data");
				for (int i = 0; i < objects.size(); i++) {
					JSONObject data = objects.getJSONObject(i);
					String date = data.get("REPORT_DATE").toString(); // 年报日期
					FinanceBaseInfoPage page = new FinanceBaseInfoPage(code,
							Integer.valueOf(date.substring(0, 10).replaceAll("-", "")));
					try {
						String noticeDate = data.get("NOTICE_DATE").toString(); // 公告日期
						page.setAnnDate(Integer.valueOf(noticeDate.substring(0, 10).replaceAll("-", "")));
					} catch (Exception e) {
					}
					try {
						Double yyzsrtbzz = data.getDouble("TOTALOPERATEREVETZ"); // 营业总收入同比增长(%)
						page.setYyzsrtbzz(yyzsrtbzz);
					} catch (Exception e) {
					}
					try {
						Double gsjlrtbzz = data.getDouble("PARENTNETPROFITTZ"); // 归属净利润同比增长(%)
						page.setGsjlrtbzz(gsjlrtbzz);
					} catch (Exception e) {
					}
					try {
						Double kfjlrtbzz = data.getDouble("KCFJCXSYJLRTZ"); // 扣非净利润同比增长(%)
						page.setKfjlrtbzz(kfjlrtbzz);
					} catch (Exception e) {
					}

					try {
						Long yyzsr = CurrencyUitl.covertToLong(data.get("TOTALOPERATEREVE").toString()); // 营业总收入
						page.setYyzsr(yyzsr);
					} catch (Exception e) {
					}
					try {
						Long gsjlr = CurrencyUitl.covertToLong(data.get("PARENTNETPROFIT").toString()); // 归属净利润
						page.setGsjlr(gsjlr);
					} catch (Exception e) {
					}
					try {
						Long kfjlr = CurrencyUitl.covertToLong(data.get("KCFJCXSYJLR").toString()); // 扣非净利润
						page.setKfjlr(kfjlr);
					} catch (Exception e) {
					}
					try {
						Double jqjzcsyl = data.getDouble("ROEJQ"); // 加权净资产收益率(%)
						page.setJqjzcsyl(CurrencyUitl.roundHalfUp(jqjzcsyl));
						page.setSyldjd(CurrencyUitl.roundHalfUp(jqjzcsyl / (double) page.getQuarter()));
					} catch (Exception e) {
					}
					try {
						Double mgjyxjl = data.getDouble("MGJYXJJE");
						page.setMgjyxjl(CurrencyUitl.roundHalfUp(mgjyxjl));
					} catch (Exception e) {
					}
					try {
						Double mll = data.getDouble("XSMLL");
						page.setMll(CurrencyUitl.roundHalfUp(mll));
					} catch (Exception e) {
					}
					try {
						Double zcfzl = data.getDouble("ZCFZL");
						page.setZcfzl(zcfzl);
					} catch (Exception e) {
					}

					list.add(page);
					if (list.size() >= FETCH_CNT) {
						break;
					}
				}
				if (list.size() > 0) {
					// String dates = EastmoneyZcfzbSpider.getDates(code, list.size());
					String dates = EastmoneyZcfzbSpider.getDates(list);
					if (dates == null) {
						throw new RuntimeException("未获取到dates参数");
					}
					Map<String, FinanceBaseInfoPage> djdMap = this.danjidu(code);// 单季度：扣非，业绩牛
					Map<String, FinanceZcfzb> fzb = EastmoneyZcfzbSpider.getZcfzb(code, companyType, dates);// 现金流
					Map<String, FinanceZcfzb> llb = EastmoneyZcfzbSpider.getXjllb(code, companyType, dates);// 负债表
					for (FinanceBaseInfoPage page : list) {
						// 单季度：扣非，业绩牛
						FinanceBaseInfoPage djd = djdMap.get(page.getId());
						if (djd != null) {
							page.setDjdKf(djd.getDjdKf());
							page.setDjdKfTbzz(djd.getDjdKfTbzz());
						}
						// 现金流
						FinanceZcfzb llba = llb.get(page.getId());
						if (llba != null) {
							page.setJyxjlce(llba.getGoodWill());// yxjlce:经营现金流量差额， GoodWill:零时字段
						}
						// 负债表
						FinanceZcfzb zcfzb = fzb.get(page.getId());
						if (zcfzb != null) {
							page.setDataOk(true);
							// 基础数
							page.setGoodWill(zcfzb.getGoodWill());
							page.setIntangibleAsset(zcfzb.getIntangibleAsset());
							page.setSumAsset(zcfzb.getSumAsset());
							page.setSumDebt(zcfzb.getSumDebt());
							page.setNetAsset(zcfzb.getNetAsset());
							page.setInventory(zcfzb.getInventory());
							page.setSumDebtLd(zcfzb.getSumDebtLd());
							page.setNetAsset(zcfzb.getNetAsset());
							page.setMonetaryFund(zcfzb.getMonetaryFund());
							page.setAccountrec(zcfzb.getAccountrec());
							page.setAccountPay(zcfzb.getAccountPay());
							page.setRetaineDearning(zcfzb.getRetaineDearning());
							page.setInterestPay(zcfzb.getInterestPay());
							page.setSumLasset(zcfzb.getSumLasset());
							page.setTradeFinassetNotfvtpl(zcfzb.getTradeFinassetNotfvtpl());
							page.setStborrow(zcfzb.getStborrow());
							page.setLtborrow(zcfzb.getLtborrow());
							page.setTaxPayable(zcfzb.getTaxPayable());

							// 分析数据
							if (page.getNetAsset() > 0) {
								// 商誉-净资产
								if (page.getGoodWill() > 0) {
									page.setGoodWillRatioNetAsset(CurrencyUitl
											.roundHalfUpWhithPercent(page.getGoodWill() / page.getNetAsset()));
								}
								// 库存对应的净资产的占比
								if (page.getInventory() > 0) {
									page.setInventoryRatio(CurrencyUitl
											.roundHalfUpWhithPercent(page.getInventory() / page.getNetAsset()));
								}
							}
							page.setFundNotOk(0);
							// 资金紧张: 货币资金不能覆盖流动负债,则看比例
							if ((page.getMonetaryFund() + page.getTradeFinassetNotfvtpl() - page.getSumDebtLd()) < 0) {
								if (CurrencyUitl.cutProfit((page.getMonetaryFund() + page.getTradeFinassetNotfvtpl()),
										page.getSumDebtLd()) > 30) {
									page.setFundNotOk(1);
								}
							}
							// 应付利息较高
							if (page.getInterestPay() > 0) {// 应付利息:如果较高
								double jieqian = page.getInterestPay() * 10;// 大概借的钱=利息*10倍（10%的利息）
								if (jieqian > Yi10) {// 借钱超过10亿
									if (page.getMonetaryFund() > Yi10 || page.getRetaineDearning() > Yi10
											|| (page.getMonetaryFund() + page.getRetaineDearning()) > Yi10) {
										// 如果货币资金和未分配利润较高，明明有钱为什么借钱，
										page.setFundNotOk2(1);
									}
								}
							}
							// 财务“三高”
							double totalAmt = page.getMonetaryFund() + page.getTradeFinassetNotfvtpl();
							double totalBorrow = page.getStborrow() + page.getLtborrow();
							if ((totalAmt > Yi10) && (totalBorrow > Yi10)) {
								page.setFundNotOk3(1);
							}
							// 是否有破产风险：应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
							page.setBustUpRisks(page.getNetAsset() - page.getAccountPay() > 0 ? 0 : 1);
//							货币资金：在会计科目上，主要指库存现金、银行存款和其他货币资金三者。
//							库存现金：指存放于企业财会部门、由出纳人员经管的货币。
//							银行存款：指企业存入银行或其他金融机构的各种款项。
//							其他货币资金：指企业的银行汇票存款、银行本票存款、信用卡存款、信用证保证金存款、存出投资款、外埠存款等其他货币资金。
//						实务中主要核算各项保证金和存入支付宝等第三方支付平台的款项。
							//
//						现金及现金等价物对流动性要求更高，需是3个月内可以使用的，所以，需要在在货币资金的基础上剔除一些受限资产。
							// 应收账款-占比 同行业
							if (page.getSumLasset() > 0) {
								page.setAccountrecRatio(CurrencyUitl
										.roundHalfUpWhithPercent(page.getAccountrec() / page.getSumLasset()));
							}
						} else {
							int t = YearQuarter(page.getYear(), page.getQuarter());// ->2020*10=20200,+1=20201;
							boolean isok = true;
							for (FinanceBaseInfoPage p : list) {
								if (YearQuarter(p.getYear(), p.getQuarter()) > t) {
									isok = false;
								}
							}
							if (isok) {
								page.setDataOk(true);
							}
						}
					}
				}
				return list;
			} catch (Exception e) {
				e.printStackTrace();
			}
			ThreadsUtil.sleepRandomSecBetween15And30(trytime);
		} while (trytime <= 10);
//		WxPushUtil.pushSystem1("东方财富-财务-抓包出错,code=" + code);
		return Collections.emptyList();
	}

	public static int YearQuarter(int year, int quarter) {
		return ((year * 10) + quarter);
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

	private JSONObject getHttpRest(String url1, String start) {
		int i = 0;
		while (true) {
			try {
				String result = HttpUtil.doGet2(url1);
				result = result.substring(start.length());
				result = result.substring(0, result.length() - 1);
				JSONObject objects = JSON.parseObject(result);
				return objects;
			} catch (Exception e) {
				e.printStackTrace();
				i++;
				if (i > 3) {
					throw new RuntimeException(e);
				}
				ThreadsUtil.sleepRandomSecBetween15And30();
			}
		}
	}

	private void getYjkbByPage(String date1, List<FinYjkb> list) {
		removeKb(date1);
		// String last = redisUtil.get(RedisConstant.RDS_FIN_KUAIBAO_ + date1);
		// last = last == null ? "" : last;
//		String lastFromPage = null;
		boolean chkIndor = true;

		int page = 1;
		int trytime = 0;
		int tot_count = 0;
		int act_count = 0;
		do {
			ThreadsUtil.sleepRandomSecBetween1And5();
			String url1 = getYjkbUrl(date1, page);
			JSONObject objects = getHttpRest(url1, "var BEzQbtii=");

			if (objects.getBooleanValue("success")) {
				JSONArray datas = objects.getJSONObject("result").getJSONArray("data");
				if (tot_count <= 0) {
					tot_count = objects.getJSONObject("result").getIntValue("count");
					log.info(date1 + " 快报页面获取到总数:" + tot_count);
				}

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
							.parseInt(DateUtil.formatYYYYMMDD(DateUtil.parseDate(date, DateUtil.YYYY_MM_DD_HH_MM_SS))));
					fy.setAnnDate(Integer.parseInt(
							DateUtil.formatYYYYMMDD(DateUtil.parseDate(anndate, DateUtil.YYYY_MM_DD_HH_MM_SS))));
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

//					if (chkIndor) {
//						if (last.equals(fy.getCode())) {
//							chkIndor = false;
//						} else {
//							list.add(fy);
//						}
//					}
//					if (lastFromPage == null) {
//						lastFromPage = fy.getCode();
//					}
					log.info(fy);
					list.add(fy);
					act_count++;
				}
				if (tot_count > 0 && act_count >= tot_count) {
					chkIndor = false;
				}
				page++;
			} else {
				log.info(date1 + " 快报页面未获取到总数");
				trytime++;
				if (trytime >= 3) {
					chkIndor = false;
				}
				if (act_count > 0 && page > 1) {// 已经最后一页，无数据
					chkIndor = false;
				}
				if (tot_count > 0 && act_count >= tot_count) {
					chkIndor = false;
				}
			}
		} while (chkIndor);
		// 设置最新
		// if (lastFromPage != null) {
		// redisUtil.set(RedisConstant.RDS_FIN_KUAIBAO_ + date1, lastFromPage);
		// }
	}

	private String getYjkbUrl(String date, int page) {
		return "http://datacenter.eastmoney.com/api/data/get?type=RPT_FCI_PERFORMANCEE&sty=ALL&p=" + page
				+ "&ps=5000&st=UPDATE_DATE,SECURITY_CODE&sr=-1,-1&var=BEzQbtii&filter=(REPORT_DATE=%27" + date
				+ "%27)&rt=" + System.currentTimeMillis();
	}

	private void getYjygByPage(String date1, List<FinYjyg> list) {
		removeYg(date1);
//		String last = redisUtil.get(RedisConstant.RDS_FIN_YUGAO_ + date1);
//		last = last == null ? "" : last;
//		String lastFromPage = null;
		boolean chkIndor = true;
		int page = 1;
		int trytime = 0;
		int tot_count = 0;
		int act_count = 0;
		do {
			ThreadsUtil.sleepRandomSecBetween1And5();
			String url1 = getYjygUrl(date1, page);
			JSONObject objects = getHttpRest(url1, "var MRtZkjmw=");
			if (objects.getBooleanValue("success")) {
				JSONArray datas = objects.getJSONObject("result").getJSONArray("data");
				if (tot_count <= 0) {
					tot_count = objects.getJSONObject("result").getIntValue("count");
					log.info(date1 + " 预告页面获取到总数:" + tot_count);
				}
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
							.parseInt(DateUtil.formatYYYYMMDD(DateUtil.parseDate(date, DateUtil.YYYY_MM_DD_HH_MM_SS))));
					fy.setAnnDate(Integer.parseInt(
							DateUtil.formatYYYYMMDD(DateUtil.parseDate(anndate, DateUtil.YYYY_MM_DD_HH_MM_SS))));
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

//					if (chkIndor) {
//						if (last.equals(fy.getCode())) {
//							chkIndor = false;
//						} else {
//							list.add(fy);
//						}
//					}
//					if (lastFromPage == null) {
//						lastFromPage = fy.getCode();
//					}
					log.info(fy);
					list.add(fy);
					act_count++;
				}
				page++;
				if (tot_count > 0 && act_count >= tot_count) {
					chkIndor = false;
				}
			} else {
				log.info(date1 + " 预告页面未获取到总数");
				trytime++;
				if (trytime >= 3) {
					chkIndor = false;
				}
				if (act_count > 0 && page > 1) {// 已经最后一页，无数据
					chkIndor = false;
				}
				if (tot_count > 0 && act_count >= tot_count) {
					chkIndor = false;
				}
			}
		} while (chkIndor);
		// 设置最新
//		if (lastFromPage != null) {
//			redisUtil.set(RedisConstant.RDS_FIN_YUGAO_ + date1, lastFromPage);
//		}
	}

	private String getYjygUrl(String date, int page) {
		return "http://datacenter.eastmoney.com/api/data/get?type=RPT_PUBLIC_OP_PREDICT&sty=ALL&p=" + page
				+ "&ps=5000&st=NOTICE_DATE,SECURITY_CODE&sr=-1,-1&var=MRtZkjmw&filter=(REPORTDATE=%27" + date
				+ "%27)(IsLatest=%22T%22)&rt=" + System.currentTimeMillis();
	}

	private void removeKb(String date1) {
		int date = DateUtil.convertDate2(date1);
		int year = DateUtil.getYear(date);
		int jidu = DateUtil.getJidu(date);

		List<FinYjkb> kbs = getLastFinaceKbByReportDate(year, jidu);
		if (kbs != null && kbs.size() > 0) {
			for (FinYjkb kb : kbs) {
				kb.setIsValid(0);
			}
			esFinYjkbDao.saveAll(kbs);
		}
	}

	private void removeYg(String date1) {
		int date = DateUtil.convertDate2(date1);
		int year = DateUtil.getYear(date);
		int jidu = DateUtil.getJidu(date);

		List<FinYjyg> kbs = getLastFinaceYgByReportDate(year, jidu);
		if (kbs != null && kbs.size() > 0) {
			for (FinYjyg kb : kbs) {
				kb.setIsValid(0);
			}
			esFinYjygDao.saveAll(kbs);
		}
	}

	Pageable pageable = PageRequest.of(0, 9999);

	private List<FinYjkb> getLastFinaceKbByReportDate(int year, int jidu) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("year").gt(year));
		bqb.must(QueryBuilders.rangeQuery("quarter").gt(jidu));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<FinYjkb> page = esFinYjkbDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<FinYjyg> getLastFinaceYgByReportDate(int year, int jidu) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("year").gt(year));
		bqb.must(QueryBuilders.rangeQuery("quarter").gt(jidu));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<FinYjyg> page = esFinYjygDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	private String urlbase = "https://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/Index?type=web&code=%s";

	private int getcompanyType(String code) {
		String url = String.format(urlbase, formatCode2(code));
		try {
			log.info("{} df company type", code);
			String s = htmlunitSpider.getHtmlPageFromUrlWithoutJs(url).getElementById("hidctype").getAttribute("value");
			int r = Integer.valueOf(s);
			if (r > 0) {
				stockBasicService.synDwcfCompanyType(code, r);
				return r;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			htmlunitSpider.close();
		}
		return 0;
	}

	public void getDwcfCompanyTypeByJob() {
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithSort();
		for (StockBaseInfo s : list) {
			if (s.getDfcwCompnayType() <= 0) {
				getcompanyType(s.getCode());
			}
		}
	}

	private Map<String, FinanceBaseInfoPage> danjidu(String code) {
		String url = String.format(financeUrl, 2, formatCode2(code));
		String result = HttpUtil.doGet2(url);
		JSONObject jsonobj = JSON.parseObject(result);
		JSONArray objects = jsonobj.getJSONArray("data");
		Map<String, FinanceBaseInfoPage> m = new HashMap<String, FinanceBaseInfoPage>();
		for (int i = 0; i < objects.size(); i++) {
			JSONObject data = objects.getJSONObject(i);
			String date = data.get("REPORT_DATE").toString(); // 年报日期
			FinanceBaseInfoPage page = new FinanceBaseInfoPage(code,
					Integer.valueOf(date.substring(0, 10).replaceAll("-", "")));
			try {
				Double djdKf = data.getDouble("DEDU_PARENT_PROFIT"); // 单季度：扣非净利润
				page.setDjdKf(djdKf);
			} catch (Exception e) {
			}
			try {
				Double djdKfTbzz = data.getDouble("DPNP_YOY_RATIO"); // 单季度： 扣非净利润同比增长(%)
				page.setDjdKfTbzz(djdKfTbzz);
			} catch (Exception e) {
			}
			m.put(page.getId(), page);
		}
		return m;
	}

	public static void main(String[] args) {
		EastmoneySpider es = new EastmoneySpider();
		es.htmlunitSpider = new HtmlunitSpider();
		String code = "002895";
		int beforeChkDate = 99999999;
//		List<FinanceBaseInfoPage> l = es.getNewFinanceAnalysis(code, 4);
//		for (FinanceBaseInfoPage f : l) {
//			System.err.println(f);
//		}
//		String result = HttpUtil.doGet2(yjygBase);
//		EastmoneySpider es = new EastmoneySpider();
//		es.getFinYjkb();
		// 公司类型：1券商，2保险,3银行，4企业
		List<FinanceBaseInfoPage> l = es.getNewFinanceAnalysis(code, 4, beforeChkDate);
		for (FinanceBaseInfoPage r : l) {
			System.err.println(r);
		}
//		System.err.println(es.getcompanyType(code));
		// System.err.println(YearQuarter(2021, 1));
	}
}
