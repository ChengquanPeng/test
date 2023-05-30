
package com.stable.service.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.service.ConceptService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.PlateService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.eastmoney.DzjySpider;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.spider.realtime.RealTime;
import com.stable.spider.realtime.RealtimeCall;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.TagUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.OnlineMsg;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.http.req.ModelReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RunModelService {
	@Autowired
	private DzjySpider emDzjySpider;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private WebModelService webModelService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private CodeModelKLineService codeModelKLineService;
	@Autowired
	private PlateService plateService;
	@Value("${html.folder}")
	private String htmlFolder;
	@Value("${tick.folder}")
	private String tickFolder;

	@Autowired
	private MonitorPoolService monitorPoolService;

	private int tradeDate;

//	@javax.annotation.PostConstruct
	public void test() {
		new Thread(new Runnable() {
			public void run() {
				int date = 20230529;
//				codeModelKLineService.runKLineModel1(date);
//				ThreadsUtil.sleepRandomSecBetween15And30();
//				codeModelService.runModel1(date, false);
//				ThreadsUtil.sleepRandomSecBetween15And30();
				log.info("大宗交易-预警");
//				monitorPoolService.jobDzjyWarning();
				ThreadsUtil.sleepRandomSecBetween15And30();
				codeModelService.runModel1(date, false);
				ThreadsUtil.sleepRandomSecBetween15And30();
				plateService.getPlateStat();
				ThreadsUtil.sleepRandomSecBetween15And30();
				printModelHtml();
				log.info("RunModelService Done");
				System.err.println("runModel1 done");
			}
		}).start();
	}

	public synchronized void runModel(int date, boolean isweekend) {
		log.info("CodeModel processing request date={}", date);
		if (!tradeCalService.isOpen(date)) {
			date = tradeCalService.getPretradeDate(date);
		}
		log.info("Actually processing request date={}", date);
		tradeDate = date;
		stockBasicService.recashToRedis();

		if (isweekend) {
			// 周末,先跑基本面在跑技术面
			codeModelService.runModel1(date, true);
			ThreadsUtil.sleepRandomSecBetween15And30();
			codeModelKLineService.runKLineModel1(date);
		} else {
			codeModelKLineService.runKLineModel1(date);
			ThreadsUtil.sleepRandomSecBetween15And30();
			String dateYYYY_ = DateUtil.formatYYYYMMDD2(DateUtil.parseDate(date));
			log.info("大宗交易");
			emDzjySpider.byDaily(dateYYYY_);
			log.info("大宗交易-预警");
			monitorPoolService.jobDzjyWarning();
			ThreadsUtil.sleepRandomSecBetween15And30();
			codeModelService.runModel1(date, false);
		}

		ThreadsUtil.sleepRandomSecBetween15And30();
		plateService.getPlateStat();
		ThreadsUtil.sleepRandomSecBetween15And30();
		printModelHtml();
		log.info("RunModelService Done");
	}

	public void printModelHtml() {
		// 大旗形
		ModelReq mr = new ModelReq();
		mr.setDibuqixing(1);//
		mr.setPls(3);
		List<CodeBaseModel2> dqx = webModelService.getList(mr, EsQueryPageUtil.queryPage9999);
		// 小旗型
		ModelReq mr22 = new ModelReq();
		mr22.setDibuqixing2(1);
		mr22.setPls(3);
		List<CodeBaseModel2> xqx = webModelService.getList(mr22, EsQueryPageUtil.queryPage9999);
		// 十字星
		ModelReq mr2 = new ModelReq();
		mr2.setZyxing(1);
		mr2.setPls(3);
		List<CodeBaseModel2> genListTe = webModelService.getList(mr2, EsQueryPageUtil.queryPage9999);
		// V1-洗盘
		ModelReq mr3 = new ModelReq();
		mr3.setXipan(1);
		mr3.setPls(3);
		List<CodeBaseModel2> xp = webModelService.getList(mr3, EsQueryPageUtil.queryPage9999);
		// N-洗盘
		ModelReq mr4 = new ModelReq();
		mr4.setNxipan(1);
		mr4.setPls(3);
		List<CodeBaseModel2> nxp = webModelService.getList(mr4, EsQueryPageUtil.queryPage9999);

		if (dqx == null) {
			dqx = new LinkedList<>();
		}
		if (xqx == null) {
			xqx = new LinkedList<>();
		}
		if (genListTe == null) {
			genListTe = new LinkedList<>();
		}
		if (xp == null) {
			xp = new LinkedList<>();
		}
		if (nxp == null) {
			nxp = new LinkedList<>();
		}

		printHtml(dqx, xqx, genListTe, xp, nxp);
	}

	public void printOnlineHtml(List<OnlineMsg> list) {
		String htmlnamet = "online.html";
		FileWriteUitl fw = new FileWriteUitl(htmlFolder + htmlnamet, true);
		StringBuffer sb = new StringBuffer();
		// 更新时间
		sb.append("<div>在线监听-更新时间:").append(DateUtil.getTodayYYYYMMDDHHMMSS()).append("</div>");
		// table
		sb.append("<br/><table border='1' cellspacing='0' cellpadding='0'>");
		// head
		sb.append("<tr><th>序号</th><th>代码</th><th>推送消息</th><th>涨幅</th><th>股价</th></tr>");
		int size = list.size();
		if (size > 0) {
			for (int j = 0; j < size; j++) {
				OnlineMsg om = list.get(j);
				String code = om.getCode();
				RealTime rt = RealtimeCall.get(code);
				sb.append("<tr><td>").append(om.getIndex()).append("</td>");
				sb.append("<td>").append(stockBasicService.getCodeName2(code)).append("</td>");
				sb.append("<td>").append(om.getTitle()).append("</td>");
				sb.append("<td>").append(CurrencyUitl.cutProfit(rt.getYesterday(), rt.getNow())).append("%</td>");
				sb.append("<td>").append(rt.getNow()).append("</td></tr>");
			}
		} else {
			sb.append("<tr><td>无数据...</td></tr>");
		}
		sb.append("</table>");// end
		fw.writeLine(sb.toString());
		fw.close();
	}

	private void printHtml(List<CodeBaseModel2> dqx, List<CodeBaseModel2> xqx, List<CodeBaseModel2> genListTe,
			List<CodeBaseModel2> xp, List<CodeBaseModel2> nxp) {
		String htmlnamet = "qf.html";
		FileWriteUitl fw = new FileWriteUitl(htmlFolder + htmlnamet, true);
		StringBuffer sb = new StringBuffer();
		// 更新时间
		sb.append("<div>更新时间:").append(DateUtil.getTodayYYYYMMDDHHMMSS()).append("，<br/>特别十字星：人工,大宗,大票定增,做小做底+业绩不错")
				.append("<br/>确：大宗超5%，小底-大宗，小底-减持").append("<br/>十字星：K线形态，前期有洗盘[002752]，或者阴跌后[山东路桥，中国化学]</div>");
		// table
		sb.append("<br/><table border='1' cellspacing='0' cellpadding='0'>");
		// head
		sb.append(
				"<tr><th>序号</th><th>简称-代码</th><th>逻辑模型</th><th>底部类型</th><th>K线形态</th><th>特征</th><th>买点</th><th>备注</th><th>板块概念</th></tr>");
		fw.writeLine(sb.toString());
		sb = new StringBuffer();
		sb.append(this.getHtml(genListTe, true));
		fw.writeLine(sb.toString());

		List<CodeBaseModel2> ren = new LinkedList<CodeBaseModel2>();// 人工
		List<CodeBaseModel2> dq = new LinkedList<CodeBaseModel2>();// 大旗形
		List<CodeBaseModel2> xq = new LinkedList<CodeBaseModel2>();// 小旗形
		List<CodeBaseModel2> dapiao = new LinkedList<CodeBaseModel2>();// 底部未涨大票
		List<CodeBaseModel2> other = new LinkedList<CodeBaseModel2>();// 其他
		List<CodeBaseModel2> all = new LinkedList<CodeBaseModel2>();

		for (CodeBaseModel2 c : dqx) {
			if (c.getPls() == 1) {
				ren.add(c);
			} else if (c.getShooting11() > 0) {
				dapiao.add(c);
			} else if (c.getDibuQixing() > 0) {
				dq.add(c);
			} else if (c.getDibuQixing2() > 0) {
				xq.add(c);
			} else {
				other.add(c);
			}
		}
		for (CodeBaseModel2 c : xqx) {
			if (c.getPls() == 1) {
				ren.add(c);
			} else if (c.getShooting11() > 0) {
				dapiao.add(c);
			} else if (c.getDibuQixing() > 0) {
				dq.add(c);
			} else if (c.getDibuQixing2() > 0) {
				xq.add(c);
			} else {
				other.add(c);
			}
		}
		all.addAll(ren);
		all.addAll(dq);
		all.addAll(xq);
		all.addAll(dapiao);
		all.addAll(other);

		sb = new StringBuffer();
		sb.append(this.getHtml(nxp, false));// N型洗盘
		fw.writeLine(sb.toString());

		sb = new StringBuffer();
		sb.append(this.getHtml(all, false));// 其他
		fw.writeLine(sb.toString());

		sb = new StringBuffer();
		sb.append(this.getHtml(xp, false));// v1洗盘
		sb.append("</table>");// end
		fw.writeLine(sb.toString());
		fw.close();
	}

	private String getHtml(List<CodeBaseModel2> genListTe, boolean isTe) {
		StringBuffer sb = new StringBuffer("");
		// data2
		String line = "";
		String line2 = "";
		String dz = "<font color='red'>[确]</font>";
		String rg = "<font color='blue'>[人]</font>";
		if (genListTe != null && genListTe.size() > 0) {
			for (int i = 0; i < genListTe.size(); i++) {
				line = "";
				line2 = "";
				CodeBaseModel2 p1 = genListTe.get(i);
				String code = p1.getCode();
				MonitorPoolTemp cp = monitorPoolService.getMonitorPoolById(Constant.MY_ID, code);
				StockBaseInfo sbsb = stockBasicService.getCode(code);

				// 序号
				sb.append("<tr><td>").append(i + 1).append("</td>");

				// 简称-代码
				sb.append("<td><a target='_blank' href='/web/admin/manual.html?code=" + code
						+ "#pls'><font color='black'>").append(sbsb.getName()).append("<br/>").append(code)
						.append("</font>");
				if (p1.getPls() == 1) {
					sb.append(rg);
				}
				if (p1.getShooting2() > 0 || p1.getShooting6661() > 0) {
					sb.append(dz);
				}
				sb.append("</a></td>");
				// 逻辑
				sb.append("<td>").append(TagUtil.getSystemPoint(p1, Constant.HTML_LINE));
				if (p1.getZfjjup() > 0) {
					sb.append(p1.getZfjjup());
					if (p1.getZfjjupStable() > 0) {
						sb.append("/stable").append(p1.getZfjjupStable());
					}
					sb.append("年未大涨").append(Constant.HTML_LINE);
				}
				if (p1.getFinOK() > 0) {
					sb.append(p1.getFinOK()).append("年盈利,");
				}
				if (p1.getBousOK() > 0) {
					sb.append(p1.getBousOK()).append("年分红");
				}
				sb.append("</td>");
				// 底部类型
				sb.append("<td>").append(TagUtil.getTag(p1).replaceAll("绩", "<font color='blue'>绩</font>"))
						.append("</td>");

				// 形态
				if (isTe) {
					sb.append("<td><font color='blue'>中阳十字星-特</font></td>");//
				} else {
					sb.append("<td>").append(TagUtil.getXiPan(p1)).append("</td>");//
				}
				// 特征
				line2 = p1.getQixingStr();
				if (line2 != null && line2.contains("大") && line2.contains("上")) {
					line2 = "<font color='blue'>" + line2 + "</font>";
				}
				// 缩量
				if (p1.getQixing() > 0) {
					TradeHistInfoDaliy td = daliyTradeHistroyService.queryLastfq(code);
					if (this.todayPrickOK(td.getClosed(), td.getOpen(), td.getAmt())
							&& isSuoliang(code, p1.getQixing(), tradeDate)) {
						line2 += "<缩量十字星>";
					}
				}
				sb.append("<td>").append(line2).append("</td>");//

				// 买点
				TradeHistInfoDaliy d = null;
				if (cp.getShotPointPrice() > 0) {
					d = daliyTradeHistroyService.queryLastfq(code);
					if (d.getHigh() >= cp.getShotPointPrice()) {
						line = "<font color='red'>突破</font>价格:" + cp.getShotPointPrice() + Constant.HTML_LINE;
					}
					// 底部买点
					if (cp.getShotPointPriceLow() <= d.getLow() && d.getLow() <= cp.getShotPointPriceLow5()) {
						line += "接近旗形底部买点:[" + cp.getShotPointPriceLow() + "-" + cp.getShotPointPriceLow5() + "]"
								+ Constant.HTML_LINE;
					}
				}
				// 十字星
				if (cp.getShotPointPriceSzx() > 0) {
					if (d == null) {
						d = daliyTradeHistroyService.queryLastfq(code);
					}
					if (d.getClosed() >= cp.getShotPointPriceSzx()) {
						if (StringUtils.isNotBlank(line)) {
							line = line + ",<font color='red'>突破</font>十字星";
						} else {
							line = "<font color='red'>突破</font>十字星";
						}
					}
				}
				sb.append("<td>").append(line).append("</td>");
				// 备注
				sb.append("<td>").append(p1.getBuyRea()).append("</td>");
				// 板块概念
				sb.append("<td>").append(sbsb.getThsIndustry()).append("|")
						.append(TagUtil.getGn(conceptService.getCodeConcept(code))).append("</td>");// CD2
				sb.append("</tr>");
			}
		} else {
			sb.append("<tr><td>无数据...</td></tr>");
		}
		return sb.toString();
	}

	public void genPrdHtml(int date, List<CodeBaseModel2> list) {
		StringBuffer sb = new StringBuffer("");
		// 更新时间
		sb.append("<div>更新时间:").append(DateUtil.getTodayYYYYMMDDHHMMSS()).append(",计算最后交易日:" + date + "</div>");
		// table
		sb.append("<table border='1' cellspacing='0' cellpadding='0'>");
		// head
		sb.append("<tr><th>序号</th><th>简称-代码</th><th>板块概念</th></tr>");
		// data2
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				CodeBaseModel2 p1 = list.get(i);
				String code = p1.getCode();
				StockBaseInfo sbsb = stockBasicService.getCode(code);
				// 序号
				sb.append("<tr><td>").append(i + 1).append("</td>");
				// 简称-代码
				sb.append("<td><a href='http://quote.eastmoney.com/" + EastmoneySpider.formatCode2(code)
						+ ".html' target='_blank'>").append(sbsb.getName()).append("--").append(code)
						.append("</a></td>");
				// 板块概念
				sb.append("<td>").append(sbsb.getThsIndustry()).append("|")
						.append(TagUtil.getGn(conceptService.getCodeConcept(code))).append("</td>");// CD2
				sb.append("</tr>");
			}
		} else {
			sb.append("<tr><td>无数据...</td></tr>");
		}
		sb.append("</table>");// end

		String htmlnamet = "prd" + date + ".html";
		FileWriteUitl fw = new FileWriteUitl(tickFolder + htmlnamet, true);
		fw.writeLine(sb.toString());
		fw.close();
	}

	// 是否十字星
	private boolean todayPrickOK(double close, double open, double amt) {
		if (close == open) {
			if (amt < CurrencyUitl.YI_N_DOUBLE) {
				return true;
			}
		} else {
			double p = CurrencyUitl.cutProfit(open, close);
			if (-1.05 <= p && p <= 1.05) {// 收盘在1.05之间
				return true;
			}
		}
		return false;
	}

	private boolean isSuoliang(String code, int start, int end) {
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, start, end,
				EsQueryPageUtil.queryPage9999, SortOrder.DESC);
		double lowVol = Integer.MAX_VALUE;
		double max = 0.0;
		for (int i = 0; i < list.size(); i++) {
			TradeHistInfoDaliy t = list.get(i);
			if (t.getTodayChangeRate() >= -9 && t.getVolume() < lowVol) {// 跌停不算
				lowVol = t.getVolume();
			}
			if (t.getVolume() > max) {
				max = t.getVolume();
			}
		}
		if (lowVol * 3 < max) {
			return true;
		}
		return false;
	}
}
