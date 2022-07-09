
package com.stable.service.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.service.ConceptService;
import com.stable.service.PlateService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.utils.DateUtil;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.TagUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.req.ModelReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RunModelService {
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

	public synchronized void runModel(int date, boolean isweekend) {
		log.info("CodeModel processing request date={}", date);
		if (!tradeCalService.isOpen(date)) {
			date = tradeCalService.getPretradeDate(date);
		}
		log.info("Actually processing request date={}", date);

		if (isweekend) {
			// 周末,先跑基本面在跑技术面
			codeModelService.runModel1(date, true);
			ThreadsUtil.sleepRandomSecBetween15And30();
			codeModelKLineService.runKLineModel1(date);
		} else {
			codeModelKLineService.runKLineModel1(date);
			ThreadsUtil.sleepRandomSecBetween15And30();
			codeModelService.runModel1(date, false);
		}

		ThreadsUtil.sleepRandomSecBetween15And30();
		plateService.getPlateStat();
		ThreadsUtil.sleepRandomSecBetween15And30();
		printModelHtml();
	}

	private void printModelHtml() {
		ModelReq mr = new ModelReq();
		mr.setQb(1);
		mr.setPls(-1);
		List<CodeBaseModel2> genList = webModelService.getList(mr, EsQueryPageUtil.queryPage9999);

		ModelReq mr2 = new ModelReq();
		mr2.setZyxingt(1);
		mr2.setPls(-1);
		List<CodeBaseModel2> genListTe = webModelService.getList(mr2, EsQueryPageUtil.queryPage9999);
		printHtml(genList, genListTe);
	}

	private void printHtml(List<CodeBaseModel2> newList, List<CodeBaseModel2> genListTe) {
		String htmlnamet = "qf.html";
		StringBuffer sb = new StringBuffer();
		// 更新时间
		sb.append("<div>更新时间:").append(DateUtil.getTodayYYYYMMDDHHMMSS()).append("</div><br/>");
		// table
		sb.append("起飞<br/><table border='1' cellspacing='0' cellpadding='0'>");
		// head
		sb.append(
				"<tr><th>序号</th><th>简称-代码</th><th>逻辑模型</th><th>起飞形态</th><th>Desc</th><th>底部类型</th><th>板块概念</th></tr>");
		// data2
		if (genListTe != null && genListTe.size() > 0) {
			for (int i = 0; i < genListTe.size(); i++) {
				CodeBaseModel2 p1 = genListTe.get(i);
				String code = p1.getCode();
				StockBaseInfo sbsb = stockBasicService.getCode(code);
				sb.append("<tr><td>").append(i + 1).append("</td>");// 序号
				sb.append("<td>").append(sbsb.getName()).append("-").append(code).append("</td>");// 简称-代码
				sb.append("<td>").append(TagUtil.getSystemPoint(p1, Constant.HTML_LINE)).append("</td>");// 逻辑
				sb.append("<td>中阳十字星-特</td>");//
				sb.append("<td>").append(p1.getQixingStr()).append("</td>");//
				sb.append("<td>").append(TagUtil.getTag(p1)).append("</td>");//
				sb.append("<td>").append(sbsb.getThsIndustry()).append("|")
						.append(TagUtil.getGn(conceptService.getCodeConcept(code))).append("</td>");// CD2
				sb.append("</tr>");
			}
		} else {
			sb.append("<tr><td>无数据1</td></tr>");
		}
		// data1
		if (newList != null && newList.size() > 0) {
			for (int i = 0; i < newList.size(); i++) {
				CodeBaseModel2 p1 = newList.get(i);
				String code = p1.getCode();
				StockBaseInfo sbsb = stockBasicService.getCode(code);
				sb.append("<tr><td>").append(i + 1).append("</td>");// 序号
				sb.append("<td>").append(sbsb.getName()).append("-").append(code).append("</td>");// 简称-代码
				sb.append("<td>").append(TagUtil.getSystemPoint(p1, Constant.HTML_LINE)).append("</td>");// 逻辑
				sb.append("<td>").append(TagUtil.getQif(p1)).append("</td>");//
				sb.append("<td>").append(p1.getQixingStr()).append("</td>");//
				sb.append("<td>").append(TagUtil.getTag(p1)).append("</td>");//
				sb.append("<td>").append(sbsb.getThsIndustry()).append("|")
						.append(TagUtil.getGn(conceptService.getCodeConcept(code))).append("</td>");// CD2
				sb.append("</tr>");
			}
		} else {
			sb.append("<tr><td>无数据1</td></tr>");
		}
		// end
		sb.append("</table>");
		FileWriteUitl fw = new FileWriteUitl(htmlFolder + htmlnamet, true);
		fw.writeLine(sb.toString());
		fw.close();
	}
}
