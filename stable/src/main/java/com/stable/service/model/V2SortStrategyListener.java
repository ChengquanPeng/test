package com.stable.service.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.stable.config.SpringConfig;
import com.stable.service.ConceptService.ConceptInfo;
import com.stable.service.StockBasicService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LineTickData;
import com.stable.service.model.data.LineVol;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class V2SortStrategyListener implements StrategyListener {
	private String header = "<table border='1' cellspacing='0' cellpadding='0'><tr>";
	private String endder = "</table><script type='text/javascript' src='/tkhtml/static/addsinaurl.js'></script>";

	private Map<String, ModelContext> map = new HashMap<String, ModelContext>();
	private List<ModelContext> cxts = new LinkedList<ModelContext>();

	// 白马：长期30日上方，回踩30日线后，突然放量上涨的

	List<ModelV1> saveList = new LinkedList<ModelV1>();
	private int treadeDate;

	public void processingModelResult(ModelContext cxt, LineAvgPrice lineAvgPrice, LinePrice linePrice, LineVol lineVol,
			LineTickData lineTickData) {

		ModelV1 mv = new ModelV1();
		mv.setCode(cxt.getCode());
		mv.setDate(cxt.getDate());
		mv.setModelType(2);
		mv.setId(mv.getModelType() + mv.getCode() + mv.getDate());

		boolean isOk = false;
		if (cxt.isBaseDataOk()) {
			int avgScore = 0;
			int strongScore = 0;
			int pgmScore = 0;
			int wayScore = 0;
			int gnScore = 0;

			// 均线
			try {
				if (lineAvgPrice.feedData()) {
					if (lineAvgPrice.isAvgSort20T30()) {// 20和30日均F各均线
						if (lineAvgPrice.isWhiteHorse()) {
							mv.setWhiteHorse(1);// 白马？
//							if (lineVol.moreVol()) {// 3日量
//								avgScore += 10;
//							}
							DaliyBasicInfo today = cxt.getToday();
							StockAvg todayAv = lineAvgPrice.todayAv;
							if (todayAv.getAvgPriceIndex20() > today.getLow()
									&& today.getClose() > todayAv.getAvgPriceIndex5()) {
								isOk = true;
								avgScore = 100;
							}
						}

					} else {
						isOk = false;
						cxt.setDropOutMsg("均线不满足要求");
					}
				} else {
					isOk = false;
					cxt.setDropOutMsg("未获取到均价");
				}
			} catch (Exception e) {
				isOk = false;
				cxt.setDropOutMsg("获取到均价异常");
				e.printStackTrace();
				ErrorLogFileUitl.writeError(e, "均线执行异常", "", "");
			}

			if (isOk) {
				strongScore = linePrice.strongScore();

				lineTickData.tickDataInfo();
				pgmScore = cxt.getSortPgm() * 5;// 3.程序单
				wayScore = cxt.getSortWay() * 5;// 4.交易方向

				// 概念板块
				List<ConceptInfo> list = cxt.getGnDaliy().get(mv.getCode());
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						ConceptInfo x = list.get(i);
						gnScore += x.getRanking();
						cxt.addGnStr(x.toString());
					}
				}

			} else {
				cxts.add(cxt);
			}
			mv.setAvgScore(avgScore);
			mv.setSortStrong(strongScore);
			mv.setSortPgm(pgmScore);
			mv.setSortWay(wayScore);
			mv.setGnScore(gnScore);
			mv.setPriceIndex(cxt.getPriceIndex());
			mv.setScore(avgScore + strongScore + pgmScore + wayScore + gnScore);
			if (isOk) {
				saveList.add(mv);
			}
			map.put(cxt.getCode(), cxt);
		} else {
			cxts.add(cxt);
		}
	}

	// **评分

	public V2SortStrategyListener() {
		String[] s = { "序号", "代码", "简称", "日期", "综合评分", "均线价格", "短期强势", "主力行为", "主动买入", "价格指数", "概念详情", "评分详情" };
		for (int i = 0; i < s.length; i++) {
			header += this.getHTMLTH(s[i]);
		}
		header += "</tr>" + FileWriteUitl.LINE_FILE;
	}

	public void fulshToFile() {
		log.info("saveList size:{}", saveList.size());
		if (saveList.size() > 0) {
			treadeDate = saveList.get(0).getDate();
			StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
			sort(saveList);
			SpringConfig efc = SpringUtil.getBean(SpringConfig.class);

			StringBuffer sb = new StringBuffer(header);
			StringBuffer sb2 = new StringBuffer(header);
			int index = 1;

			for (ModelV1 mv : saveList) {

				sb2.append("<tr>").append(getHTML(index)).append(getHTML_SN(mv.getCode()))
						.append(getHTML(sbs.getCodeName(mv.getCode()))).append(getHTML(mv.getDate()))
						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
						.append(getHTML(mv.getSortWay())).append(getHTML(mv.getPriceIndex()))
						.append(getHTML(map.get(mv.getCode()).getGnStr()))
						.append(getHTML(map.get(mv.getCode()).getDetailDescStr())).append("</tr>")
						.append(FileWriteUitl.LINE_FILE);
				index++;
			}
			sb.append(endder);
			sb2.append(endder);

			String filepath2 = efc.getModelV1SortFloderDesc() + "sort_v2_prv_" + treadeDate + ".html";
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

	private void sort(List<ModelV1> set) {
		Collections.sort(set, new Comparator<ModelV1>() {
			@Override
			public int compare(ModelV1 o1, ModelV1 o2) {
				return o2.getScore() - o1.getScore();
			}
		});
	}

	@Override
	public List<ModelV1> getResultList() {
		return saveList;
	}
}
