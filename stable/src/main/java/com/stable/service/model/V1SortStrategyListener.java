package com.stable.service.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class V1SortStrategyListener implements StrategyListener {
	private String header = "<table border='1' cellspacing='0' cellpadding='0'><tr>";
	private String endder = "</table><script type='text/javascript' src='/tkhtml/static/addsinaurl.js'></script>";

	// 不OK List
	private List<ModelContext> mcs = Collections.synchronizedList(new LinkedList<ModelContext>());

	// OK List
	List<ModelV1> saveList = Collections.synchronizedList(new LinkedList<ModelV1>());

	// OK和不OK结果
	private Map<String, String> result = new ConcurrentHashMap<String, String>();

	// 均线横盘突破,放量上涨:
	// 价格排除:x涨太多;x上影线;x大幅高开低走(不包括收盘5日线上);
	// 量排除: x换手率太高或者太低; 放量超过30日均量50%

	private int treadeDate;

	public V1SortStrategyListener(int date) {
		this.treadeDate = date;
	}

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
		boolean isOk = true;
		if (StringUtils.isBlank(mc.getBaseDataOk())) {
			int avgScore = 0;
			int strongScore = 0;
			int pgmScore = 0;
			int wayScore = 0;
			int gnScore = 0;

			// 均线
			try {
				if (lineAvgPrice.feedData()) {
					if (lineAvgPrice.isAvgSort20T30()) {// 20和30日均F各均线
						setDetail(detailDesc, "30日均线排列base20T30");
						avgScore += 20;
						if (lineAvgPrice.isAvgSort3T30()) {// 各均线排列整齐
							setDetail(detailDesc, "30日各均线排列整齐3T30");
							avgScore += 5;
						}

						if (lineAvgPrice.is5Don30Dhalf()) {// 是否5日均线在30日线上，超过15天,排除下跌周期中，刚开始反转的均线
							dropOutMsg = "30个交易日中，30日均线在5日均线上方多日";
							isOk = false;
						} else {
							if (lineAvgPrice.isWhiteHorse()) {
								setDetail(detailDesc, "白马？");
								mv.setWhiteHorse(1);// 白马？
							}
							if (lineAvgPrice.isRightUp()) {// 横盘突破:需要看量
								setDetail(detailDesc, "横盘突破? ");
								avgScore += 5;
								String rs1 = lineVol.moreVol();
								if (StringUtils.isNotBlank(rs1)) {// 3日量
									avgScore += 10;
									setDetail(detailDesc, rs1);
								}
								String rs2 = lineVol.moreVolWithAvg();
								if (StringUtils.isNotBlank(rs2)) {// 30日均量
									avgScore += 10;
									setDetail(detailDesc, rs2);
								}
							}
							mc.setBase30Avg(true);
						}
					} else {
						isOk = false;
						dropOutMsg = "均线不满足要求";
					}
				} else {
					isOk = false;
					dropOutMsg = "未获取到均价-30D";
				}
			} catch (Exception e) {
				isOk = false;
				dropOutMsg = "获取到均价异常";
				e.printStackTrace();
				ErrorLogFileUitl.writeError(e, "均线执行异常", "", "");
			}

			if (isOk) {
				// 量
				int r2 = lineVol.isHighOrLowVolToday();
				if (r2 != 0) {// 换手率超过30%或者低于2%
					if (r2 == 1) {
						dropOutMsg = "换手率超过30%";
					} else {
						dropOutMsg = "换手率低于2%";
					}
					isOk = false;
				}
				// 价格
				if (linePrice.isHighOrLowVolToday()) {
					// 排除上影线,
					dropOutMsg = "上影线";
					isOk = false;
				}
				if (linePrice.isHignOpenWithLowCloseToday()) {
					// 排除高开低走
					dropOutMsg = "高开低走";
					isOk = false;
				}
				if (linePrice.isRange20pWith20days()) {
					// 20天波动超过20%
					dropOutMsg = "20天波动超过20%";
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
				wayScore = mc.getSortWay() * 5;// 4.交易方向

				// 概念板块
				List<ConceptInfo> list = mc.getGnDaliy().get(mv.getCode());
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						ConceptInfo x = list.get(i);
						gnScore += x.getRanking();
						setDetail(detailDesc, x.toString());
					}
				}
				mv.setAvgScore(avgScore);
				mv.setSortStrong(strongScore);
				mv.setSortPgm(pgmScore);
				mv.setSortWay(wayScore);
				mv.setGnScore(gnScore);
				mv.setPriceIndex(mc.getPriceIndex());
				mv.setScore(avgScore + strongScore + pgmScore + wayScore + gnScore);

				result.put(mv.getCode(), detailDesc.toString());
				saveList.add(mv);
			} else {
				result.put(mv.getCode(), dropOutMsg);
				mcs.add(mc);
			}
		} else {
			result.put(mv.getCode(), mc.getBaseDataOk());
			mcs.add(mc);
		}
	}

	// **评分

	public V1SortStrategyListener() {
		String[] s = { "序号", "代码", "简称", "日期", "综合评分", "均线价格", "短期强势", "主力行为", "主动买入", "价格指数", "评分详情" };
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
			StringBuffer sb2 = new StringBuffer(header);
			int index = 1;

			for (ModelV1 mv : saveList) {
				String code = mv.getCode();
				sb.append("<tr>").append(getHTML(index)).append(getHTML_SN(code)).append(getHTML(sbs.getCodeName(code)))
						.append(getHTML(mv.getDate())).append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
						.append(getHTML(mv.getSortWay())).append(getHTML(mv.getPriceIndex())).append(getHTML(""))
						.append("</tr>").append(FileWriteUitl.LINE_FILE);

				sb2.append("<tr>").append(getHTML(index)).append(getHTML_SN(code))
						.append(getHTML(sbs.getCodeName(code))).append(getHTML(mv.getDate()))
						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgScore()))
						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
						.append(getHTML(mv.getSortWay())).append(getHTML(mv.getPriceIndex()))
						.append(getHTML(result.get(code))).append("</tr>").append(FileWriteUitl.LINE_FILE);
				index++;
			}
			sb.append(endder);
			sb2.append(endder);

//			String filepath = efc.getModelV1SortFloder() + "sort_v1_" + treadeDate + ".html";
//			FileWriteUitl fw = new FileWriteUitl(filepath, true);
//			fw.writeLine(sb.toString());
//			fw.close();

			String filepath2 = efc.getModelV1SortFloderDesc() + "sort_v1_" + treadeDate + ".html";
			FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
			fw2.writeLine(sb2.toString());
			fw2.close();
		}

		fulshToFile2();
	}

	private void fulshToFile2() {
		StockBasicService sbs = SpringUtil.getBean(StockBasicService.class);
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		String filepath2 = efc.getModelV1SortFloderDesc() + "sort_v1_dropout_" + treadeDate + ".html";
		StringBuffer sb2 = new StringBuffer(
				"<table border='1' cellspacing='0' cellpadding='0'><tr><th>seq</th><th>code</th><th>名称</th><th>分数</th><th>均线排列(20)</th><th>原因</th></tr>"
						+ FileWriteUitl.LINE_FILE);
		log.info("mcs:size:" + mcs.size());
		for (int i = 0; i < mcs.size(); i++) {
			// log.info("currIndex:{},totoal:{}", i, mcs.size());
			ModelContext mc = mcs.get(i);
			String code = mc.getCode();
			sb2.append("<tr>").append(getHTML(i)).append(getHTML_SN(code))// 代码
					.append(getHTML(sbs.getCodeName(code)))// 名称
					.append(getHTML(mc.getScore()))// 分数
					.append(getHTML(mc.isBase30Avg()))// 至少30日均线排列
					.append(getHTML(result.get(code))).append("</tr>").append(FileWriteUitl.LINE_FILE);// 原因
		}
		sb2.append(endder);

		FileWriteUitl fw2 = new FileWriteUitl(filepath2, true);
		fw2.writeLine(sb2.toString());
		fw2.close();
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
