package com.stable.service.model;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.EsCodeBaseModelHistDao;
import com.stable.es.dao.base.EsFinanceBaseInfoHyDao;
import com.stable.service.AnnouncementService;
import com.stable.service.ChipsService;
import com.stable.service.CodePoolService;
import com.stable.service.ConceptService;
import com.stable.service.FinanceService;
import com.stable.service.PlateService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.data.FinanceAnalyzer;
import com.stable.utils.BeanCopy;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.AnnMentParamUtil;
import com.stable.vo.bus.AnnouncementHist;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.CodePool;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.FinanceBaseInfoHangye;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZhiYa;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelService {
	@Autowired
	private AnnouncementService announcementService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private EsFinanceBaseInfoHyDao esFinanceBaseInfoHyDao;
	@Autowired
	private EsCodeBaseModelHistDao codeBaseModelHistDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private PlateService plateService;
	@Autowired
	private ChipsService chipsService;
	@Autowired
	private ZhiYaService zhiYaService;

	public synchronized void runJobv2(boolean isJob, int date) {
		try {
			runByJobv2(date);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "CodeModel模型运行异常", "", "");
			WxPushUtil.pushSystem1("CodeModel模型运行异常..");
		}
	}

	private synchronized void runByJobv2(int tradeDate) {
		Date now = new Date();
		oneYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));
		halfYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -180));
		log.info("CodeModel processing request date={}", tradeDate);
		if (!tradeCalService.isOpen(tradeDate)) {
			tradeDate = tradeCalService.getPretradeDate(tradeDate);
		}
		log.info("Actually processing request date={}", tradeDate);
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
		List<CodeBaseModelHist> listHist = new LinkedList<CodeBaseModelHist>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		Map<String, CodeBaseModel2> histMap = getALLForMap();
		for (StockBaseInfo s : codelist) {
			try {
				getBaseAnalyse(s, tradeDate, histMap.get(s.getCode()), listLast, listHist);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
			}
		}
		if (listLast.size() > 0) {
			codeBaseModel2Dao.saveAll(listLast);
		}
		if (listHist.size() > 0) {
			codeBaseModelHistDao.saveAll(listHist);
		}
//		middleSortV1Service.start(tradeDate, list);
		log.info("CodeModel v2 模型执行完成");
		WxPushUtil.pushSystem1(
				"Seq5=> CODE-MODEL " + tradeDate + " 共[" + codelist.size() + "]条,今日更新条数:" + listHist.size());
	}

	private void getBaseAnalyse(StockBaseInfo s, int tradeDate, CodeBaseModel2 oldOne, List<CodeBaseModel2> listLast,
			List<CodeBaseModelHist> listHist) {
		String code = s.getCode();
		log.info("Code Model  processing for code:{}", code);
		CodeBaseModel2 newOne = new CodeBaseModel2();
		newOne.setCode(code);
		newOne.setDate(tradeDate);
		listLast.add(newOne);
		// 财务
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(code, tradeDate,
				EsQueryPageUtil.queryPage9999);
		if (fbis == null) {
			boolean onlineYear = stockBasicService.online1YearChk(code, tradeDate);
			if (onlineYear) {
				ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, tradeDate + "", "Code Model错误");
			} else {
				log.info("{},Online 上市不足1年", code);
			}
			return;
		}
		FinanceAnalyzer fa = new FinanceAnalyzer();
		for (FinanceBaseInfo fbi : fbis) {
			fa.putJidu1(fbi);
		}
		FinanceBaseInfo fbi = fa.getCurrJidu();
		newOne.setCurrYear(fbi.getYear());
		newOne.setCurrQuarter(fbi.getQuarter());

		// ======== 红色警告 ========
		StringBuffer sb1 = new StringBuffer();
		// 退市风险
		if (fa.profitDown2Year() == 1) {
			newOne.setBaseRed(1);
			sb1.append("退市风险:2年利润亏损").append(Constant.HTML_LINE);
		}
		if (fbi.getBustUpRisks() == 1) {
			newOne.setBaseRed(1);
			sb1.append("破产风险:负债超高-净资产低于应付账款").append(Constant.HTML_LINE);
		}
		if (fbi.getFundNotOk() == 1) {
			newOne.setBaseRed(1);
			sb1.append("资金紧张:流动负债高于货币资金").append(Constant.HTML_LINE);
		}
		if (fbi.getNetAsset() < 0) {
			newOne.setBaseRed(1);
			sb1.append("净资产小于0").append(Constant.HTML_LINE);
		}
		// ======== 黄色警告 ========
		StringBuffer sb2 = new StringBuffer();
		if (fa.getCurrYear().getGsjlr() < 0) {
			newOne.setBaseYellow(1);
			sb2.append("年报亏损").append(Constant.HTML_LINE);
		}
		if (fbi.getFundNotOk2() == 1) {
			newOne.setBaseYellow(1);
			sb2.append("资金紧张:应付利息较高()").append(Constant.HTML_LINE);
		}
		// 毛利，应收账款
		FinanceBaseInfoHangye hy = this.getFinanceBaseInfoHangye(code, fbi.getYear(), fbi.getQuarter());
		if (hy != null) {
			if (hy.getMll() > 0 && hy.getMll() > hy.getMllAvg() && hy.getMllRank() <= 5) {
				newOne.setBaseYellow(1);
				sb2.append("(需人工复核)毛利率:" + hy.getMll() + " 行业平均:" + hy.getMllAvg() + ", 行业排名:" + hy.getMllRank())
						.append(Constant.HTML_LINE);
			}
			if (hy.getYszk() > 0 && hy.getYszk() > hy.getYszkAvg() && hy.getYszkRank() <= 5) {
				newOne.setBaseYellow(1);
				sb2.append("(需人工复核)应收账款:" + hy.getYszk() + " 行业平均:" + hy.getYszkAvg() + ", 行业排名:" + hy.getYszkRank())
						.append(Constant.HTML_LINE);
			}
		}
		// 商誉占比
		if (fbi.getGoodWillRatioNetAsset() > 0.15) {// 超过15%
			newOne.setBaseYellow(1);
			sb2.append("商誉占比超15%:" + (fbi.getGoodWillRatioNetAsset() * 100) + "%").append(Constant.HTML_LINE);
		}
		// 库存占比
		if (fbi.getInventoryRatio() > 0.45) {// 超过50%
			newOne.setBaseYellow(1);
			sb2.append("库存占比超45%:" + (fbi.getInventoryRatio() * 100) + "%").append(Constant.HTML_LINE);
		}
		// 股东增持（一年）
		AnnouncementHist zengchi = announcementService.getLastRecordType(code, AnnMentParamUtil.zhengchi.getType(),
				oneYearAgo);
		if (zengchi != null) {
			newOne.setBaseYellow(1);
			sb2.append("股东/高管增持:" + (zengchi.getRptDate()) + "%").append(Constant.HTML_LINE);

		}
		// 高质押
		ZhiYa zy = zhiYaService.getZhiYa(code);
		if (zy.getHasRisk() == 1) {//
			newOne.setBaseYellow(1);
			sb2.append("高质押风险:" + zy.getDetail()).append(Constant.HTML_LINE);
		}
		// 分红
		FenHong fh = chipsService.getFenHong(code);
		if (fh.getTimes() <= 0) {
			newOne.setBaseYellow(1);
			sb2.append("无分红记录").append(Constant.HTML_LINE);
		}
		// 限售股解禁（2年：前后1年）
		List<Jiejin> jj = chipsService.getBf2yearJiejin(code);
		if (jj != null) {
			newOne.setBaseYellow(1);
			sb2.append("前后1年解禁记录数:" + jj.size()).append(Constant.HTML_LINE);
		}

		// ======== 蓝色警告 && 绿色 ========
		StringBuffer sb3 = new StringBuffer();
		StringBuffer sb4 = new StringBuffer();
		if (fa.getCurrJidu().getYyzsrtbzz() <= 0) {
			newOne.setBaseBlue(1);
			sb3.append("营收同比下降").append(Constant.HTML_LINE);
		} else {
			newOne.setBaseGreen(1);
			sb4.append("营收同比上涨").append(Constant.HTML_LINE);
		}
		if (fa.getCurrJidu().getGsjlrtbzz() <= 0) {
			newOne.setBaseBlue(1);
			sb3.append("净利同比下降").append(Constant.HTML_LINE);
		} else {
			newOne.setBaseGreen(1);
			sb4.append("净利同比上涨").append(Constant.HTML_LINE);
		}
		evaluateStep1(newOne, fa, fbis);
		if (newOne.getSylType() == 4 || newOne.getSylType() == 1) {
			newOne.setBaseGreen(1);
			sb4.append("资产收益率上涨").append(Constant.HTML_LINE);
		} else {
			newOne.setBaseBlue(1);
			sb3.append("资产收益率下降").append(Constant.HTML_LINE);
		}
		if (newOne.getSylType() == 4 || newOne.getSylType() == 2) {
			newOne.setBaseGreen(1);
			sb4.append("资产收益率年超20%").append(Constant.HTML_LINE);
		}
		// 正在增发中
		chkZf(newOne);
		if (newOne.getZfStatus() == 1 || newOne.getZfStatus() == 2) {
			newOne.setBaseBlue(1);
			sb3.append("增发进度" + (s.getCompnayType() == 1 ? "(国资)" : "") + ":" + newOne.getZfStatusDesc())
					.append(Constant.HTML_LINE);
		}
		// 股东减持（一年）
		AnnouncementHist jianchi = announcementService.getLastRecordType(code, AnnMentParamUtil.jianchi.getType(),
				oneYearAgo);
		if (jianchi != null) {
			newOne.setBaseBlue(1);
			sb2.append("股东/高管减持:" + (jianchi.getRptDate()) + "%").append(Constant.HTML_LINE);
		}
		// 回购（半年内）
		AnnouncementHist huigou = announcementService.getLastRecordType(code, AnnMentParamUtil.huigou.getType(),
				halfYearAgo);
		if (huigou != null) {
			newOne.setBaseBlue(1);
			sb2.append("回购:" + (huigou.getRptDate()) + "%").append(Constant.HTML_LINE);
		}

		if (newOne.getBaseRed() > 0) {
			newOne.setBaseRedDesc(sb1.toString());
		} else {
			newOne.setBaseRedDesc("");
		}
		if (newOne.getBaseYellow() > 0) {
			newOne.setBaseYellowDesc(sb2.toString());
		} else {
			newOne.setBaseYellowDesc("");
		}
		if (newOne.getBaseBlue() > 0) {
			newOne.setBaseBlueDesc(sb3.toString());
		} else {
			newOne.setBaseBlueDesc("");
		}
		if (newOne.getBaseGreen() > 0) {
			newOne.setBaseGreenDesc(sb4.toString());
		} else {
			newOne.setBaseGreenDesc("");
		}
		newOne.setId(code);

		boolean saveHist = true;
		if (oldOne != null) {
			// 复制一些属性
			newOne.setMonitor(oldOne.getMonitor());

			// 是否更新历史
			if (oldOne.getKeyString().equals(newOne.getKeyString())) {// 主要指标是否有变化，则记录
				saveHist = false;
			}
		}
		if (saveHist) {
			// copy history
			CodeBaseModelHist hist = new CodeBaseModelHist();
			BeanCopy.copy(newOne, hist);
			hist.setId(code + tradeDate);
			listHist.add(hist);
		}
	}

	private int oneYearAgo = 0;// 一年以前
	private int halfYearAgo = 0;// 半年以前

//	private synchronized void runByJob(int tradeDate) {
//		Date now = new Date();
//		oneYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));
//		halfYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -180));
////		end = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, 370));
//		log.info("CodeModel processing request date={}", tradeDate);
//		if (!tradeCalService.isOpen(tradeDate)) {
//			tradeDate = tradeCalService.getPretradeDate(tradeDate);
//		}
//		log.info("Actually processing request date={}", tradeDate);
//		int updatedate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
//		List<CodeBaseModel> listLast = new LinkedList<CodeBaseModel>();
//		List<CodeBaseModelHist> listHist = new LinkedList<CodeBaseModelHist>();
//		int oneYearAgo = DateUtil.getPreYear(tradeDate);
//		int nextYear = DateUtil.getNextYear(tradeDate);
//		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
//		Map<String, CodeBaseModel> histMap = getALLForMap();
//		Map<String, CodePool> map = codePoolService.getCodePoolMap();
//		List<CodePool> list = new LinkedList<CodePool>();
//		for (StockBaseInfo s : codelist) {
//			try {
//				getSorce(s, tradeDate, oneYearAgo, nextYear, updatedate, listLast, listHist, true,
//						histMap.get(s.getCode()), list, map);
//			} catch (Exception e) {
//				ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
//			}
//		}
//		if (listLast.size() > 0) {
//			codeBaseModelDao.saveAll(listLast);
//		}
//		if (listHist.size() > 0) {
//			codeBaseModelHistDao.saveAll(listHist);
//		}
//		middleSortV1Service.start(tradeDate, list);
//		log.info("CodeModel 模型执行完成");
//		WxPushUtil.pushSystem1(
//				"Seq5=> CODE-MODEL " + tradeDate + " 共[" + codelist.size() + "]条,今日更新条数:" + listHist.size());
//
//	}

//	private void getSorce(StockBaseInfo s, int treadeDate, int oneYearAgo, int nextYear, int updatedate,
//			List<CodeBaseModel> listLast, List<CodeBaseModelHist> listHist, boolean isJob, CodeBaseModel lastOne,
//			List<CodePool> list, Map<String, CodePool> map) {
//
//		// 业绩快报(准确的)
//		FinYjkb yjkb = financeService.getLastFinaceKbByReportDate(code);
//		boolean hasKb = false;
//
//		// 业绩快报(准确的)
//		if (yjkb != null) {
//			if ((newOne.getCurrYear() == yjkb.getYear() && newOne.getCurrQuarter() < yjkb.getQuarter())// 同一年，季度大于
//					|| (yjkb.getYear() > newOne.getCurrYear())) {// 不同年
//				newOne.setForestallYear(yjkb.getYear());
//				newOne.setForestallQuarter(yjkb.getQuarter());
//				newOne.setForestallIncomeTbzz(yjkb.getYyzsrtbzz());
//				newOne.setForestallProfitTbzz(yjkb.getJlrtbzz());
//				hasKb = true;
//			}
//		}
//		// 业绩预告(类似天气预报,可能不准)
//		FinYjyg yjyg = null;
//		if (!hasKb) {
//			yjyg = financeService.getLastFinaceYgByReportDate(code);
//			if (yjyg != null) {
//				if ((newOne.getCurrYear() == yjyg.getYear() && newOne.getCurrQuarter() < yjyg.getQuarter())// 同一年,季度大于
//						|| (yjyg.getYear() > newOne.getCurrYear())) {// 不同年
//					newOne.setForestallYear(yjyg.getYear());
//					newOne.setForestallQuarter(yjyg.getQuarter());
//					newOne.setForestallProfitTbzz(yjyg.getJlrtbzz());
//				}
//			}
//		}
//		processingFinance(list, map, newOne, lastOne, fa, fbis);
//		if (bb != null) {
//			// 股东大会通过/实施/完成
//		} else {
//			newOne.setNoBackyear(-1);// 最近1年无回购
//		}
//		if (dh == null) {
//			newOne.setNoDividendyear(-1);// 最近1年无分红
//		}
//
//		// 正向分数
//		int finals = 0;
//		// ======营收同比增长======
//		if (fbi.getYyzsrtbzz() > 0) {
//			finals += 20;// 营收同比增长
//		} else {
//			if (fbi.getYyzsrtbzz() > -10) {
//				finals += -5;
//			} else if (fbi.getYyzsrtbzz() > -20) {
//				finals += -10;
//			} else {
//				finals += -20;
//			}
//		}
//
//		// ======利润======
//		// 归属净利
//		if (fbi.getGsjlr() > 0) {
//			finals += 5;//
//		} else {// 亏损
//			finals += -5;
//		}
//		// 扣非净利
//		if (fbi.getKfjlr() > 0) {
//			finals += 5;// 营收同比增长
//		} else {// 亏损
//			finals += -5;
//		}
//		// 归属净利同比
//		if (fbi.getGsjlrtbzz() > 0) {
//			finals += 5;//
//		} else {// 亏损
//			finals += -5;
//		}
//		// 扣非净利同比
//		if (fbi.getKfjlrtbzz() > 0) {
//			finals += 5;// 营收同比增长
//		} else {// 亏损
//			finals += -5;
//		}
//
//		// ======资产收益率======
//		// 收益率类型:1:自身收益率增长,2: 年收益率超过5.0%*4=20%,4:同时包含12
//		if (newOne.getSylType() == 1) {
//			finals += 30;
//		} else if (newOne.getSylType() == 2) {
//			finals += 20;
//		} else if (newOne.getSylType() == 4) {
//			finals += 40;
//		}
//
//		// 分红(3)
//		if (newOne.getLastDividendDate() != 0) {
//			finals += 3;
//		} else {
//			finals += -2;
//		}
//		// 回购(3)
//		if (newOne.getLastBackDate() != 0) {
//			finals += 3;
//		} else {
//			finals += -1;
//		}
//		// 质押
//		if (newOne.getZyRask() == 1) {
//			finals += -5;
//		}
//		// 解禁
////		int shareFloat = 0;
////		if (newOne.getFloatDate() > 0) {
////			shareFloat = -3;
////		}
//		newOne.setScore(finals);
//		newOne.setUdpateDate(updatedate);
//
//		boolean saveHist = true;
//		if (lastOne != null) {// 评分变化和季度
//			if (lastOne.getScore() == newOne.getScore() && lastOne.getCurrQuarter() == newOne.getCurrQuarter()) {
//				saveHist = false;
//			} else {
//				newOne.setUpScore(finals - lastOne.getScore());
//			}
//		}
//		// 其他
//		newOne.setHolderChange(chipsService.holderNumAnalyse(code));
//
//		newOne.setId(code);
//		listLast.add(newOne);
//		if (saveHist) {
//			// copy history
//			CodeBaseModelHist hist = new CodeBaseModelHist();
//			BeanCopy.copy(newOne, hist);
//			hist.setId(code + treadeDate);
//			listHist.add(hist);
//		}
//	}

	// 增发
	private void chkZf(CodeBaseModel2 newOne) {
		ZengFa zengfa = chipsService.getLastZengFa(newOne.getCode());
		// start 一年以前
		if (zengfa != null && (zengfa.getStartDate() > oneYearAgo || zengfa.getEndDate() > oneYearAgo
				|| zengfa.getZjhDate() > oneYearAgo)) {
			newOne.setZfStatus(zengfa.getStatus());
			newOne.setZfStatusDesc(zengfa.getStatusDesc());
		} else {
			newOne.setZfStatus(0);
			newOne.setZfStatusDesc("");
		}
	}

	private void evaluateStep1(CodeBaseModel2 newOne, FinanceAnalyzer fa, List<FinanceBaseInfo> fbis) {
		FinanceBaseInfo currJidu = fa.getCurrJidu();
		newOne.setSyl(currJidu.getJqjzcsyl());
		newOne.setSylttm(plateService.getSylTtm(fbis));
		newOne.setSyldjd(plateService.getSyldjd(currJidu));// 单季度？
		if (newOne.getSyldjd() > newOne.getSylttm()) {
			newOne.setSylType(1);// 自身收益率增长
		}
		if (newOne.getSylttm() >= 5.0) {// 单季度5%，全年20%
			if (newOne.getSylType() == 1) {
				newOne.setSylType(4);// 同时
			} else {
				newOne.setSylType(2);// 年收益率超过5.0%*4=20%
			}
		}
	}

	public List<CodePool> findBigBoss(int treadeDate) {
		log.info("treadeDate={}", treadeDate);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
		Map<String, CodePool> map = codePoolService.getCodePoolMap();
		List<CodePool> list = new LinkedList<CodePool>();
		for (StockBaseInfo s : codelist) {
			try {
				List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(s.getCode(), treadeDate,
						EsQueryPageUtil.queryPage9999);
				if (fbis != null && fbis.size() > 0) {
					FinanceAnalyzer fa = new FinanceAnalyzer();
					for (FinanceBaseInfo fbi : fbis) {
						fa.putJidu1(fbi);
					}
					findBigBoss(s.getCode(), treadeDate, list, map, fbis, fa);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "", "", "");
			}
		}
		return list;
	}

	private CodePool findBigBoss(String code, int treadeDate, List<CodePool> list, Map<String, CodePool> map,
			List<FinanceBaseInfo> fbis, FinanceAnalyzer fa) {
		log.info("findBigBoss code:{}", code);
		CodePool c = map.get(code);
		if (c == null) {
			c = new CodePool();
			c.setCode(code);
		}
		list.add(c);
		c.setUpdateDate(treadeDate);
		// 是否符合中线、1.市盈率和ttm在50以内
		c.setKbygjl(0);
		c.setKbygys(0);
//		if (yjkb != null) {
//			c.setKbygys(yjkb.getYyzsrtbzz());
//			c.setKbygjl(yjkb.getJlrtbzz());
//		} else if (yjyg != null) {
//			c.setKbygjl(yjyg.getJlrtbzz());
//		}

		// 业绩连续
		int continueJidu1 = 0;
		int continueJidu2 = 0;
		boolean cj1 = true;
		int cj2 = 0;
		List<Double> high = new LinkedList<Double>();
		List<Double> high2 = new LinkedList<Double>();
		for (FinanceBaseInfo fbi : fbis) {
			if (cj1 && fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 连续季度增长
				continueJidu1++;
				high.add(fbi.getYyzsrtbzz());
			} else {
				cj1 = false;
			}
			if (cj2 <= 1 && fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 允许一次断连续
				continueJidu2++;
				high2.add(fbi.getYyzsrtbzz());
			} else {
				cj2++;
			}
		}
		boolean isok = false;
		if (continueJidu1 > 3 || continueJidu2 > 5) {
			if (continueJidu1 > 3) {
				int cn = 0;
				for (Double h : high) {// 连续超过25%的次数超过一半
					if (h > 25.0) {
						cn++;
					}
				}
				if (cn * 2 > continueJidu1) {
					isok = true;
				}
			} else if (continueJidu2 > 5) {
				int cn = 0;
				for (Double h : high2) {
					if (h > 25.0) {
						cn++;
					}
				}
				if (cn * 2 > continueJidu2) {
					isok = true;
				}
			}
		}
		c.setIsok(isok);
		c.setContinYj1(continueJidu1);
		c.setContinYj2(continueJidu2);
		return c;
	}

	public CodeBaseModel2 getLastOneByCode2(String code) {
		log.info("getLastOneByCode:{}", code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		CodeBaseModel2 cbm = new CodeBaseModel2();
		cbm.setCode(code);
		return cbm;
	}

//	public CodeBaseModel getLastOneByCode(String code) {
//		log.info("getLastOneByCode:{}", code);
//		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
//		bqb.must(QueryBuilders.matchPhraseQuery("id", code));
//		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
//		SearchQuery sq = queryBuilder.withQuery(bqb).build();
//
//		Page<CodeBaseModel> page = codeBaseModelDao.search(sq);
//		if (page != null && !page.isEmpty()) {
//			return page.getContent().get(0);
//		}
//		CodeBaseModel cbm = new CodeBaseModel();
//		cbm.setCode(code);
//		return cbm;
//	}

	public Map<String, CodeBaseModel2> getALLForMap() {
		List<CodeBaseModel2> list = getALLForList();
		Map<String, CodeBaseModel2> map = new HashMap<String, CodeBaseModel2>();
		for (CodeBaseModel2 c : list) {
			map.put(c.getCode(), c);
		}
		return map;
	}

	private List<CodeBaseModel2> getALLForList() {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	public List<CodeBaseModelHist> getListByCode(String code, EsQueryPageReq querypage) {
		log.info("getListByCode:{}", code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModelHists");
		return null;
	}

	public CodeBaseModelHist getHistOneById(String id) {
		log.info("getHistOneById:{}", id);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	private FinanceBaseInfoHangye getFinanceBaseInfoHangye(String code, int year, int quarter) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		bqb.must(QueryBuilders.matchPhraseQuery("quarter", quarter));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<FinanceBaseInfoHangye> page = esFinanceBaseInfoHyDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		} else {
//			bqb = QueryBuilders.boolQuery();
//			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
//			queryBuilder = new NativeSearchQueryBuilder();
//			FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);
//			sq = queryBuilder.withQuery(bqb).withSort(sort).build();
//			page = esFinanceBaseInfoHyDao.search(sq);
//			if (page != null && !page.isEmpty()) {
//				return page.getContent().get(0);
//			}
		}
		return null;
	}

	public List<CodeBaseModel2> getList(String code, int orderBy, String aliasCode, String conceptName, int asc,
			EsQueryPageReq querypage, String zfStatus) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else if (StringUtils.isNotBlank(aliasCode)) {
			List<String> list = conceptService.listCodesByAliasCode(aliasCode);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		} else if (StringUtils.isNotBlank(conceptName)) {
			List<String> list = listCodeByCodeConceptName(conceptName);
			if (list.size() > 0) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		}
		if (StringUtils.isNotBlank(zfStatus)) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfStatus", Integer.valueOf(zfStatus)));
		}
		String field = "baseGreen";
		if (orderBy == 3) {
			field = "sylttm";
		} else if (orderBy == 4) {
			field = "syl";
		} else if (orderBy == 5) {
			field = "sylType";
		}
//		<option value="3">资产收益率ttm</option>
//		<option value="4">资产收益率报告期</option>
//		<option value="5">资产收益评级</option>
		SortOrder order = SortOrder.DESC;
		if (asc == 2) {
			order = SortOrder.ASC;
		}

		FieldSortBuilder sort = SortBuilders.fieldSort(field).unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	public List<CodeBaseModelResp> getListForWeb(String code, int orderBy, String conceptId, String conceptName,
			int asc, EsQueryPageReq querypage, String zfStatus) {
		log.info(
				"CodeBaseModel getListForWeb code={},orderBy={},asc={},num={},size={},conceptId={},conceptName={},zfStatus={}",
				code, orderBy, asc, querypage.getPageNum(), querypage.getPageSize(), conceptId, conceptName, zfStatus);

		List<CodeBaseModel2> list = getList(code, orderBy, conceptId, conceptName, asc, querypage, zfStatus);
		List<CodeBaseModelResp> res = new LinkedList<CodeBaseModelResp>();
		if (list != null) {
			for (CodeBaseModel2 dh : list) {
				CodeBaseModelResp resp = new CodeBaseModelResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
//				resp.setIncomeShow(dh.getCurrIncomeTbzz() + "%");
//				if (dh.getForestallIncomeTbzz() > 0) {
//					resp.setIncomeShow(resp.getIncomeShow() + "(" + dh.getForestallIncomeTbzz() + "%)");
//				}
//				resp.setProfitShow(dh.getCurrProfitTbzz() + "%");
//				if (dh.getForestallIncomeTbzz() > 0) {
//					resp.setProfitShow(resp.getProfitShow() + "(" + dh.getForestallProfitTbzz() + "%)");
//				}
				res.add(resp);
			}
		}
		return res;
	}

	public List<String> listCodeByCodeConceptName(String conceptName) {
		List<String> codes = new LinkedList<String>();
		List<StockBaseInfo> l = stockBasicService.getAllOnStatusList();
		conceptName = conceptName.trim();
		for (StockBaseInfo s : l) {
			if (s.getThsIndustry().contains(conceptName)) {
				codes.add(s.getCode());
			}
		}
		return codes;
	}

}
