package com.stable.service.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.stable.config.SpringConfig;
import com.stable.constant.Constant;
import com.stable.enums.ModelType;
import com.stable.service.ConceptService.ConceptInfo;
import com.stable.service.StockBasicService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LinePrice.StrongResult;
import com.stable.service.model.data.LineTickData;
import com.stable.service.model.data.LineVol;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.Monitoring;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class V1SortStrategyListener implements StrategyListener {
	private String header = "<table border='1' cellspacing='0' cellpadding='0'><tr>";
	private String endder = "</table><script type='text/javascript' src='/tkhtml/static/addsinaurl.js'></script>";

	// 不OK List
	// private List<ModelContext> mcs = Collections.synchronizedList(new
	// LinkedList<ModelContext>());

	// OK List
	List<ModelV1> saveList = Collections.synchronizedList(new LinkedList<ModelV1>());

	// OK和不OK结果
	// private Map<String, String> result = new ConcurrentHashMap<String, String>();

	// 均线横盘突破,放量上涨:
	// 价格排除:x涨太多;x上影线;x大幅高开低走(不包括收盘5日线上);
	// 量排除: x换手率太高或者太低; 放量超过30日均量50%

	private CodeModelService codeModelService;

	private void setDetail(StringBuffer detailDesc, String desc) {
		detailDesc.append(desc).append(Constant.DOU_HAO);
	}

	public void processingModelResult(ModelContext mc, LineAvgPrice lineAvgPrice, LinePrice linePrice, LineVol lineVol,
			LineTickData lineTickData) {

		ModelV1 mv = new ModelV1();
		mv.setCode(mc.getCode());
		mv.setDate(mc.getDate());
		mv.setModelType(ModelType.V1.getCode());
		mv.setId(mv.getModelType() + mv.getCode() + mv.getDate());

		StringBuffer detailDesc = new StringBuffer();
		String dropOutMsg = "";
		boolean isOk = false;
		if (StringUtils.isBlank(mc.getBaseDataOk())) {
			int baseScore = 0;
			int strongScore = 0;
			int pgmScore = 0;
			int wayScore = 0;
			int gnScore = 0;

			// 均线
			try {
				if (lineAvgPrice.isWhiteHorseV2(mc.getCode(), mc.getDate())) {
					boolean b6 = linePrice.checkPriceBack6dayWhitTodayV2();// 5.回调过超10%
					if (b6) {// 一年未涨
						if (linePrice.oneYearCheck(mc.getCode(), mc.getDate())) {// 一年未涨
							isOk = true;
						} else {
							dropOutMsg = "未一年未涨";
						}
					} else {
						dropOutMsg = "未回调过超10%";
					}
				} else {
					dropOutMsg = "非白马";
				}
			} catch (Exception e) {
				isOk = false;
				dropOutMsg = "获取到均价异常";
				e.printStackTrace();
				ErrorLogFileUitl.writeError(e, "均线执行异常", "", "");
			}

			if (isOk) {
				// 量
				if (mc.getToday().getTurnover_rate_f() >= 20) {// 换手率超过30%
					dropOutMsg = "换手率超过20%";
					isOk = false;
				}
				// 价格
				if (linePrice.isLowClosePriceToday()) {
					// 排除上影线,
					dropOutMsg = "上影线";
					isOk = false;
				}
				if (linePrice.isHignOpenWithLowCloseToday()) {
					// 排除高开低走
					dropOutMsg = "高开低走";
					isOk = false;
				}
			}

			if (isOk) {
				if (!lineTickData.tickDataInfo()) {
					setDetail(detailDesc, "每日指标记录小于5条,checkStrong get size<5");
					dropOutMsg += "每日指标记录小于5条,checkStrong get size<5";
				}
				StrongResult sr = linePrice.strongScore();
				strongScore = sr.getStrongScore();
				if (strongScore > 0) {
					setDetail(detailDesc, sr.getStrongDetail());
				}
				pgmScore = mc.getSortPgm() * 5;// 3.程序单

				// 概念板块
				List<ConceptInfo> list = mc.getGnDaliy().get(mv.getCode());
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						ConceptInfo x = list.get(i);
						gnScore += x.getRanking();
						setDetail(detailDesc, x.toString());
					}
				}
				baseScore = codeModelService.getLastOneByCode(mc.getCode()).getScore();
				mv.setAvgScore(baseScore);
				mv.setSortStrong(strongScore);
				mv.setSortPgm(pgmScore);
				mv.setSortWay(wayScore);
				mv.setGnScore(gnScore);
				mv.setPriceIndex(mc.getPriceIndex());
				mv.setScore(baseScore + strongScore + pgmScore + wayScore + gnScore);

				// result.put(mv.getCode(), detailDesc.toString());
				saveList.add(mv);
			} else {
				log.info("code={},dropOutMsg={}", mc.getCode(), dropOutMsg);
				// result.put(mv.getCode(), dropOutMsg);
				// mcs.add(mc);
			}
		} else {
			// result.put(mv.getCode(), mc.getBaseDataOk());
			// mcs.add(mc);
		}
	}

	// **评分

	private int treadeDate;

	public V1SortStrategyListener(int date, CodeModelService codeModelService) {
		treadeDate = date;
		this.codeModelService = codeModelService;
		String[] s = { "序号", "代码", "简称", "日期", "综合评分", "基本面评分", "短期强势", "主力行为" };
		for (int i = 0; i < s.length; i++) {
			header += this.getHTMLTH(s[i]);
		}
		header += "</tr>" + FileWriteUitl.LINE_FILE;
	}

	public void fulshToFile() {
		log.info("saveList size:{}", saveList.size());
		if (saveList.size() > 0) {
			StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
			sort(saveList);
			SpringConfig efc = SpringUtil.getBean(SpringConfig.class);

			StringBuffer sb = new StringBuffer(header);
			// StringBuffer sb2 = new StringBuffer(header);
			int index = 1;

			for (ModelV1 mv : saveList) {
				String code = mv.getCode();
				sb.append("<tr>").append(getHTML(index)).append(getHTML_SN(code)).append(getHTML(sbs.getCodeName(code)))
						.append(getHTML(mv.getDate())).append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm())).append("</tr>")
						.append(FileWriteUitl.LINE_FILE);

//				sb2.append("<tr>").append(getHTML(index)).append(getHTML_SN(code))
//						.append(getHTML(sbs.getCodeName(code))).append(getHTML(mv.getDate()))
//						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
//						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
//						.append(getHTML(mv.getSortWay())).append(getHTML(mv.getPriceIndex()))
//						.append(getHTML(result.get(code))).append("</tr>").append(FileWriteUitl.LINE_FILE);
				index++;
			}
			sb.append(endder);
			// sb2.append(endder);

			String filepath = efc.getModelV1SortFloder() + "sortv1.html";
			FileWriteUitl fw = new FileWriteUitl(filepath, true);
			fw.writeLine(sb.toString());
			fw.close();

//			String filepath2 = efc.getModelV1SortFloderDesc() + "sortv1.html";
//			FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
//			fw2.writeLine(sb2.toString());
//			fw2.close();
		}

		// fulshToFile2();
	}

//	protected void fulshToFile2() {
//		StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
//		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
//		String filepath2 = efc.getModelV1SortFloderDesc() + "sort_v1_dropout_" + treadeDate + ".html";
//		StringBuffer sb2 = new StringBuffer(
//				"<table border='1' cellspacing='0' cellpadding='0'><tr><th>seq</th><th>code</th><th>名称</th><th>分数</th><th>均线排列(20)</th><th>原因</th></tr>"
//						+ FileWriteUitl.LINE_FILE);
//		log.info("mcs:size:" + mcs.size());
//		for (int i = 0; i < mcs.size(); i++) {
//			// log.info("currIndex:{},totoal:{}", i, mcs.size());
//			ModelContext mc = mcs.get(i);
//			String code = mc.getCode();
//			sb2.append("<tr>").append(getHTML(i)).append(getHTML_SN(code))// 代码
//					.append(getHTML(sbs.getCodeName(code)))// 名称
//					.append(getHTML(mc.getScore()))// 分数
//					.append(getHTML(mc.isBase30Avg()))// 至少30日均线排列
//					.append(getHTML(result.get(code))).append("</tr>").append(FileWriteUitl.LINE_FILE);// 原因
//		}
//		sb2.append(endder);
//
//		FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
//		fw2.writeLine(sb2.toString());
//		fw2.close();
//	}

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

	@Override
	public List<Monitoring> getMonitoringList() {
		List<Monitoring> ml = new LinkedList<Monitoring>();
		if (saveList.size() > 0) {
			for (ModelV1 mv : saveList) {
				Monitoring mt = new Monitoring();
				mt.setCode(mv.getCode());
				mt.setReqBuyDate(treadeDate);
				mt.setVer(1);// V1
				mt.setId(mt.getCode() + mt.getVer());// 各个模型版本监控不一样
				ml.add(mt);
			}
		}
		return ml;
	}
}
