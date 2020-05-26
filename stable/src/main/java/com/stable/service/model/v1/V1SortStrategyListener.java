package com.stable.service.model.v1;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.stable.config.SpringConfig;
import com.stable.service.StockBasicService;
import com.stable.service.model.StrategyListener;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.ModelV1context;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class V1SortStrategyListener implements StrategyListener {
	private String header = "<table border='1' cellspacing='0' cellpadding='0'><tr>";
	private String endder = "</table><script type='text/javascript' src='/tkhtml/static/addsinaurl.js'></script>";

	private List<ModelV1> set = new LinkedList<ModelV1>();
	private Map<String, ModelV1context> map = new HashMap<String, ModelV1context>();

	public void condition(Object... obj) {
		ModelV1 mv1 = (ModelV1) obj[0];
		// 短期强势
		if (mv1.getSortStrong() > 0 && mv1.getAvgIndex() >= 10 && mv1.getVolIndex() > 0) {
			set.add(mv1);
			ModelV1context wv = (ModelV1context) obj[1];
			map.put(mv1.getCode(), wv);
		}
	}

	public V1SortStrategyListener() {
		String[] s = { "序号", "代码", "简称", "日期", "综合评分", "均线价格", "短线交易量", "短期强势", "主力行为", "主动买入", "价格指数", "概念详情", "评分详情",
				"详情ID" };
		for (int i = 0; i < s.length; i++) {
			header += this.getHTMLTH(s[i]);
		}
		header += "</tr>" + FileWriteUitl.LINE_FILE;
	}

	public void fulshToFile(int treadeDate, List<ModelV1context> cxts) {
		StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		String filepath2 = efc.getModelV1SortFloderDesc() + "sort_v1_dropout_" + treadeDate + ".html";
		StringBuffer sb2 = new StringBuffer(
				"<table border='1' cellspacing='0' cellpadding='0'><tr><th>seq</th><th>code</th><th>名称</th><th>分数</th><th>入围</th><th>原因</th></tr>"
						+ FileWriteUitl.LINE_FILE);
		int index = 1;
		for (ModelV1context cxt : cxts) {
			String code = cxt.getCode();
			sb2.append("<tr>").append(getHTML(index)).append(getHTML_SN(code))// 代码
					.append(getHTML(sbs.getCodeName(code)))// 名称
					.append(getHTML(cxt.getScore()))// 分数
					.append(getHTML(map.containsKey(code)))// 入围
					.append(getHTML(cxt.getDropOutMsg())).append("</tr>").append(FileWriteUitl.LINE_FILE);// 原因
			index++;
		}
		sb2.append(endder);
		FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
		fw2.writeLine(sb2.toString());
		fw2.close();
	}

	public void fulshToFile() {
		log.info("List<ModelV1> size:{}", set.size());
		if (set.size() > 0) {
			StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
			sort();
			SpringConfig efc = SpringUtil.getBean(SpringConfig.class);

			StringBuffer sb = new StringBuffer(header);
			StringBuffer sb2 = new StringBuffer(header);
			int index = 1;

			for (ModelV1 mv : set) {
				sb.append("<tr>").append(getHTML(index)).append(getHTML_SN(mv.getCode()))
						.append(getHTML(sbs.getCodeName(mv.getCode()))).append(getHTML(mv.getDate()))
						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgIndex()))
						.append(getHTML(mv.getVolIndex())).append(getHTML(mv.getSortStrong()))
						.append(getHTML(mv.getSortPgm())).append(getHTML(mv.getSortWay()))
						.append(getHTML(mv.getPriceIndex())).append(getHTML(map.get(mv.getCode()).getGnStr()))
						.append(getHTML("")).append(getHTML(mv.getId())).append("</tr>")
						.append(FileWriteUitl.LINE_FILE);

				sb2.append("<tr>").append(getHTML(index)).append(getHTML_SN(mv.getCode()))
						.append(getHTML(sbs.getCodeName(mv.getCode()))).append(getHTML(mv.getDate()))
						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgIndex()))
						.append(getHTML(mv.getVolIndex())).append(getHTML(mv.getSortStrong()))
						.append(getHTML(mv.getSortPgm())).append(getHTML(mv.getSortWay()))
						.append(getHTML(mv.getPriceIndex())).append(getHTML(map.get(mv.getCode()).getGnStr()))
						.append(getHTML(map.get(mv.getCode()).getDetailDescStr())).append(getHTML(mv.getId()))
						.append("</tr>").append(FileWriteUitl.LINE_FILE);
				index++;
			}
			sb.append(endder);
			sb2.append(endder);

			String filepath = efc.getModelV1SortFloder() + "sort_v1_" + set.get(0).getDate() + ".html";
			FileWriteUitl fw = new FileWriteUitl(filepath, true);
			fw.writeLine(sb.toString());
			fw.close();

			String filepath2 = efc.getModelV1SortFloderDesc() + "sort_v1_prv_" + set.get(0).getDate() + ".html";
			FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
			fw2.writeLine(sb2.toString());
			fw2.close();
		}
	}

	private String getHTML(Object text) {
		return "<td>" + text + "</td>";
	}

	private String getHTML_SN(Object text) {
		return "<td class='sn'>" + text + "</td>";
	}

	private String getHTMLTH(Object text) {
		return "<th>" + text + "</th>";
	}

	private void sort() {
		Collections.sort(set, new Comparator<ModelV1>() {
			@Override
			public int compare(ModelV1 o1, ModelV1 o2) {
				return o2.getScore() - o1.getScore();
			}
		});
	}

}
