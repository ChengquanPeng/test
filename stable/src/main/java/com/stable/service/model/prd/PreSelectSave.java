package com.stable.service.model.prd;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.Prd1Dao;
import com.stable.service.ConceptService;
import com.stable.service.StockBasicService;
import com.stable.service.model.ModelWebService;
import com.stable.utils.DateUtil;
import com.stable.utils.FileWriteUitl;
import com.stable.vo.bus.Prd1;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.CodeBaseModelResp;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class PreSelectSave {

	@Autowired
	private Prd1Dao prd1Dao;
	@Autowired
	private StockBasicService stockBasicService;
	@Value("${html.folder}")
	private String htmlFolder;
	@Autowired
	private ModelWebService modelWebService;
	@Autowired
	private ConceptService conceptService;

	private String htmlname = "prd1.html";

	private String getPrdSub(int sub) {
		if (sub == 1) {
			return "1=2天下跌";
		}
		if (sub == 2) {
			return "2=3天以上下跌";
		}
		return sub + "=未知";
	}

	public void save(List<Prd1> newList) {
		// 保存到数据库
		Map<String, Prd1> hash = new HashMap<String, Prd1>();
		if (newList != null) {
			for (Prd1 p1 : newList) {
				hash.put(p1.getCode(), p1);
			}
		}
		// 基本面排除？？？TODO
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithOutSort();
		List<Prd1> insList = new LinkedList<Prd1>();
		for (StockBaseInfo s : list) {
			if (hash.containsKey(s.getCode())) {
				insList.add(hash.get(s.getCode()));
			} else {
				Prd1 p1 = new Prd1();
				p1.setCode(s.getCode());
				p1.setPrd(0);
				insList.add(p1);
			}
		}
		prd1Dao.saveAll(insList);
		log.info("Prd1 保存到数据库记录数:{}", insList.size());
		// 生成HTML
		printHtml(newList);
	}

	private void printHtml(List<Prd1> newList) {
		StringBuffer sb = new StringBuffer();
		// 更新时间
		sb.append("<div>更新时间:").append(DateUtil.getTodayYYYYMMDDHHMMSS()).append("</div><br/>");
		// table
		sb.append("<table border='1' cellspacing='0' cellpadding='0'>");
		// head
		sb.append(
				"<tr><th>序号</th><th>代码</th><th>简称</th><th>细分类</th><th>流通市值</th><th>概念</th><th>基本面</th><th>备注</th></tr>");
		// data
		if (newList != null && newList.size() > 0) {
			for (int i = 0; i < newList.size(); i++) {
				Prd1 p1 = newList.get(i);
				String code = p1.getCode();
				sb.append("<tr><td>").append(i + 1).append("</td>");// 序号
				sb.append("<td class='sn'>").append(code).append("</td>");// 代码
				sb.append("<td>").append(stockBasicService.getCodeName(code)).append("</td>");// 简称
				sb.append("<td>").append(getPrdSub(p1.getPrdsub())).append("</td>");// 细分类
				CodeBaseModelResp cbm = modelWebService.getLastOneByCodeResp(code, true);
				String bk = stockBasicService.getCode(code).getThsIndustry();// 同花顺板块
				String gn = conceptService.getCodeConceptStr(code);// 同花顺概念
				sb.append("<td>").append(cbm.getMkv()).append("亿<br/>活筹").append(cbm.getActMkv()).append("亿</td>");// 流通市值
				sb.append("<td>").append(bk + gn).append("</td>");// 概念
				sb.append("<td>").append(cbm.getBaseRedDesc()).append("</td>");// 基本面
				sb.append("<td>").append(cbm.getBuyRea()).append("<br/>").append(cbm.getZfjjInfo()).append("</td>");// 备注
				sb.append("</tr>");

			}
		} else {
			sb.append("<tr><td>无数据</td></tr>");
		}
		// end
		sb.append("</table><script type='text/javascript' src='/html/static/addsinaurl.js'></script>");
		FileWriteUitl fw = new FileWriteUitl(htmlFolder + htmlname, true);
		fw.writeLine(sb.toString());
		fw.close();
	}

}
