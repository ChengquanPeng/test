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
import com.stable.enums.MonitorType;
import com.stable.enums.ZfStatus;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.EsFinanceBaseInfoHyDao;
import com.stable.service.ConceptService;
import com.stable.service.ReducingHoldingSharesService;
import com.stable.service.StockBasicService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.TagUtil;
import com.stable.vo.ReducingHoldingSharesStat;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.FinanceBaseInfoHangye;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.req.ModelManulReq;
import com.stable.vo.http.req.ModelReq;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class WebModelService {
	@Autowired
	private EsFinanceBaseInfoHyDao esFinanceBaseInfoHyDao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private ReducingHoldingSharesService reducingHoldingSharesService;

	public static long WAN = CurrencyUitl.WAN_N.longValue();

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
		cbm.setId(code);
		cbm.setCode(code);
		return cbm;
	}

	public CodeBaseModelResp getLastOneByCodeResp(String code, boolean isMyid) {
		return getModelResp(false, getLastOneByCode2(code), isMyid);
	}

	private CodeBaseModelResp getModelResp(boolean trymsg, CodeBaseModel2 dh, boolean isMyid) {
		CodeBaseModelResp resp = new CodeBaseModelResp();
		BeanUtils.copyProperties(dh, resp);
		resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
		StockBaseInfo s = stockBasicService.getCode(dh.getCode());
		resp.setCircZb(s.getCircZb());
		resp.setBankuai(s.getThsIndustry() + "<br/> " + s.getThsLightspot());
		StringBuffer sb1 = new StringBuffer("");
		if (dh.getBaseRed() == 1) {
			sb1.append("<font color='red'>红:</font>" + dh.getBaseRedDesc());
		}
		if (dh.getBaseYellow() == 1) {
			sb1.append("<font color='#FF00FF'>黄:</font>" + dh.getBaseYellowDesc());
		}
		if (dh.getBaseBlue() == 1) {
			sb1.append("<font color='blue'>蓝:</font>" + dh.getBaseBlueDesc());
		}
		resp.setBaseInfo(sb1.toString());
		// 标签
		StringBuffer tag = new StringBuffer("");
		tag.append("<font color='red'>");
		if (dh.getQb() > 0) {
			tag.append("起飞->");
		}
		if (dh.getDibuQixing() > 0) {
			tag.append("大旗形").append(dh.getDibuQixing()).append(dh.getQixingStr()).append(Constant.HTML_LINE);
		}
		if (dh.getDibuQixing2() > 0) {
			tag.append("小旗形").append(dh.getDibuQixing2()).append(dh.getQixingStr()).append(Constant.HTML_LINE);
		}

		if (dh.getZyxing() > 0) {
			tag.append("中阳十字星").append(Constant.HTML_LINE);
		}
		tag.append("</font>");
		if (dh.getShootingw() == 1) {
			tag.append("K线攻击形态").append(Constant.HTML_LINE);
		}
		if (dh.getShooting10() > 0) {
			tag.append("接近1年新高").append(Constant.HTML_LINE);
		}
		if (dh.getSusWhiteHors() == 1) {
			tag.append("白马走势?").append(Constant.HTML_LINE);
		}
		if (dh.getTagSmallAndBeatf() > 0) {
			tag.append("小而美").append(Constant.HTML_LINE);
		}
		if (dh.getTagHighZyChance() > 0) {
			tag.append("高质押机会?").append(Constant.HTML_LINE);
		}
		if (dh.getSusBigBoss() == 1) {
			tag.append("业绩较牛?").append(Constant.HTML_LINE);
		}
		if (dh.getSortChips() == 1) {
			tag.append("拉升吸筹?").append(Constant.HTML_LINE);
		}
		if (StringUtils.isNotBlank(dh.getJsHist())) {
			tag.append("异动记录:").append(dh.getJsHist()).append(Constant.HTML_LINE);
		}
		resp.setTagInfo(tag.toString());

		ReducingHoldingSharesStat rhss = reducingHoldingSharesService.getLastStat(dh.getCode(), 0);
		resp.setZfjjInfo(TagUtil.gameInfo(dh, rhss, trymsg));

		StringBuffer sb6 = new StringBuffer();
		// 个人人工
		if (isMyid) {
			sb6.append(Constant.HTML_LINE).append(Constant.HTML_LINE);
			// 是否确定
			if (dh.getPls() == 0) {
				sb6.append("人工: 未确定");
			} else if (dh.getPls() == 1) {
				sb6.append("人工: 已确定");
			} else if (dh.getPls() == 2) {
				sb6.append("人工: 已排除 ").append(dh.getPlst());
			}
			if (dh.getMoni() > 0) {
				sb6.append("        ,已监听:").append(MonitorType.getCodeName(dh.getMoni()));
			}
			resp.setRengong(sb6.toString());
		}
		return resp;
	}

	public List<CodeBaseModelResp> getListForWeb(ModelReq mr, EsQueryPageReq querypage, long userId) {
		log.info("CodeBaseModel getListForWeb mr={}", mr);
		boolean isMyid = (userId == Constant.MY_ID);
		if (!isMyid) {
			mr.setPls(-1);// 重置非myid
		}
		List<CodeBaseModel2> list = getList(mr, querypage);
		List<CodeBaseModelResp> res = new LinkedList<CodeBaseModelResp>();
		if (list != null) {
			for (CodeBaseModel2 dh : list) {
				// 备注
				if (!isMyid) {
					dh.setBuyRea(this.monitorPoolService.getMonitorPoolById(userId, dh.getCode()).getRemark());
				}
				CodeBaseModelResp resp = getModelResp(mr.isTrymsg(), dh, isMyid);
				if (isMyid) {
					resp.setZfjjInfo(resp.getZfjjInfo() + resp.getRengong());
				}
				res.add(resp);
			}
		}
		return res;
	}

	public List<String> listCodeByCodeConceptName(String conceptName, EsQueryPageReq querypage) {
		List<String> codes = new LinkedList<String>();
		List<StockBaseInfo> l = stockBasicService.getAllOnStatusListWithSort();
		conceptName = conceptName.trim();
		for (StockBaseInfo s : l) {
			if (s.getThsIndustry() != null && s.getThsIndustry().trim().contains(conceptName)) {
				codes.add(s.getCode());
			}
		}
		return codes;
	}

	public void addPlsManual(long userId, ModelManulReq req) {
		int timemonth = req.getTimemonth();
		if (timemonth == 10) {
			String code = req.getCode();
			CodeBaseModel2 model = getLastOneByCode2(code);
			model.setBuyRea(req.getBuyRea().trim() + " " + DateUtil.formatYYYYMMDD2(new Date()));
			codeBaseModel2Dao.save(model);
			return;
		}

		int pls = req.getPls();
		if (pls != 0 && pls != 1 && pls != 2) {
			throw new RuntimeException("i != 0 && i != 1 && i != 2 ? ");
		}

		int date = -1;
		if (timemonth == 9) {
			pls = 0;
			date = 0;
		} else {
			int days = 0;
			if (timemonth == 1) {
				days = 30;
			} else if (timemonth == 2) {
				days = 60;
			} else if (timemonth == 3) {
				days = 90;
			} else if (timemonth == 4) {
				days = 180;
			} else if (timemonth == 5) {
				days = 365;
			}
			if (days > 0) {
				Date now = new Date();
				date = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, days));
			}
		}
		String code = req.getCode();
		CodeBaseModel2 model = getLastOneByCode2(code);
		String remark = req.getBuyRea().trim() + " " + DateUtil.formatYYYYMMDD2(new Date());
		if (date != 1) {
			MonitorPoolTemp pool = monitorPoolService.getMonitorPoolById(userId, code);
			if (pls == 2 || pls == 0) {// 2不在池子
				pool.setMonitor(MonitorType.NO.getCode());
				pool.setUpTodayChange(0);
				pool.setRealtime(0);
				pool.setOffline(0);
				pool.setDzjy(0);
				pool.setHolderNum(0);
				pool.setYkb(0);
				pool.setZfdone(0);
				pool.setZfdoneZjh(0);
				pool.setListenerGg(0);
				pool.setBuyLowVol(0);
//				pool.setShotPointCheck(0);
			} else if (pls == 1 && model.getPls() != 1) {// 1不在池子，且原来不等于1
				pool.setMonitor(MonitorType.MANUAL.getCode());
				pool.setUpTodayChange(3);
				pool.setRealtime(1);
				int dt = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
				pool.setDzjy(dt);
				pool.setHolderNum(dt);
				pool.setYkb(dt);
				pool.setZfdone(1);
				pool.setBuyLowVol(30);
//				pool.setShotPointCheck(1);
			}
			monitorPoolService.toSave(pool);
			// 同步监听
			if (pool.getMonitor() > MonitorType.NO.getCode()) {
				model.setMoni(pool.getMonitor());
			} else {
				model.setMoni(0);
			}
		}
		model.setPls(pls);
		model.setPlst(date);
		model.setBuyRea(remark);

		codeBaseModel2Dao.save(model);
	}

	public void rzrqm(String code, int timemonth) {
		int days = 0;
		if (timemonth == 1) {
			days = 30;
		} else if (timemonth == 2) {
			days = 60;
		} else if (timemonth == 3) {
			days = 90;
		} else if (timemonth == 4) {
			days = 180;
		} else if (timemonth == 5) {
			days = 365;
		}
		if (days > 0) {
			Date now = new Date();
			int date = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, days));
			CodeBaseModel2 model = getLastOneByCode2(code);
			model.setShooting30(date);
			model.setShooting3(0);
			codeBaseModel2Dao.save(model);
		}
	}

	public List<CodeBaseModel2> getList(ModelReq mr, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(mr.getCode())) {
			String[] cc = mr.getCode().split(",");
			if (cc.length == 1) {
				bqb.must(QueryBuilders.matchPhraseQuery("code", cc[0]));
			} else {
				bqb.must(QueryBuilders.termsQuery("code", cc));
			}
		} else if (StringUtils.isNotBlank(mr.getConceptId())) {
			List<String> list = conceptService.listCodesByAliasCode(mr.getConceptId(), EsQueryPageUtil.queryPage9999);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
				// pageYes = false;// 已经在这里分页了。不需要在下面分页。 这里查询有20条，但是结果可能没有20，可能是新股，没有进入codeModel
			} else {
				return new LinkedList<CodeBaseModel2>();
			}
		} else if (StringUtils.isNotBlank(mr.getConceptName())) {
			List<String> list = listCodeByCodeConceptName(mr.getConceptName(), EsQueryPageUtil.queryPage9999);
			if (list.size() > 0) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			} else {
				return new LinkedList<CodeBaseModel2>();
			}
		}

		if ("1".equals(mr.getMonitor())) {// 所有监听
			bqb.must(QueryBuilders.rangeQuery("moni").gte(1));
		}
		if (mr.getPls() != -1) {
			if (mr.getPls() == 3) {
				bqb.mustNot(QueryBuilders.matchPhraseQuery("pls", 2));// 包含1和2
			} else {
				bqb.must(QueryBuilders.matchPhraseQuery("pls", Integer.valueOf(mr.getPls())));// 1或者2
			}
		}
		if (StringUtils.isNotBlank(mr.getMkv())) {
			double mkv = Double.valueOf(mr.getMkv());
			if (mkv > 0) {
				bqb.must(QueryBuilders.rangeQuery("mkv").lte(mkv));
			}
		}
		if (StringUtils.isNotBlank(mr.getMkv2())) {
			double mkv2 = Double.valueOf(mr.getMkv2());
			if (mkv2 > 0) {
				bqb.must(QueryBuilders.rangeQuery("mkv").gte(mkv2));
			}
		}

		if (StringUtils.isNotBlank(mr.getRzrq1())) {
			double rzrq1 = Double.valueOf(mr.getRzrq1());
			if (rzrq1 > 0) {
				bqb.must(QueryBuilders.rangeQuery("rzrqRate").lte(rzrq1));
			}
		}
		if (StringUtils.isNotBlank(mr.getRzrq2())) {
			double rzrq2 = Double.valueOf(mr.getRzrq2());
			if (rzrq2 > 0) {
				bqb.must(QueryBuilders.rangeQuery("rzrqRate").gte(rzrq2));
			}
		}

		if (mr.getFinDbl() > 0) {// 业绩暴涨
			bqb.must(QueryBuilders.rangeQuery("finDbl").gt(0));
		}
		if (mr.getFinOK() > 0) {
			bqb.must(QueryBuilders.rangeQuery("finOK").gte(mr.getFinOK()));
		}
		if (mr.getBousOK() > 0) {
			bqb.must(QueryBuilders.rangeQuery("bousOK").gte(mr.getBousOK()));
		}
		if (mr.getSusBigBoss() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("susBigBoss", 1));// 基本面疑似大牛
		}
		if (mr.getFinanceInc() > 0) {
			bqb.must(QueryBuilders.rangeQuery("financeInc").gte(1));// 业绩连续增长
		}

		if (mr.getTagIndex() > 0) {
			if (mr.getTagIndex() == 1) {
				bqb.must(QueryBuilders.matchPhraseQuery("sortChips", 1));// 吸筹-收集筹码短线
			} else if (mr.getTagIndex() == 5) {
				bqb.must(QueryBuilders.matchPhraseQuery("tagSmallAndBeatf", 1));// 小而美
			} else if (mr.getTagIndex() == 4) {
				bqb.must(QueryBuilders.matchPhraseQuery("tagHighZyChance", 1));// 高质押机会
			}
		}
		if (mr.getShooting1() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting1", 1));
		}
		if (mr.getShooting2() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting2", 1));
		}
		if (mr.getShooting3() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting3", 1));
		}
		if (mr.getShooting4() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting4", 1));
		}
		if (mr.getShooting5() == 1) {
			bqb.must(QueryBuilders.rangeQuery("shooting5").gte(1));// 这是一个时间值
		}
		if (mr.getShooting6661() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting6661", 1));
		}
		if (mr.getShooting6() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting6", 1));
		}
		if (mr.getShooting7() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting7", 1));
		}
		if (mr.getShooting8() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting8", 1));
		}
		if (mr.getShooting9() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting9", 1));
		}
		// 技术面
		if ("1".equals(mr.getPre1Year())) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting10", 1));
		}
		if ("1".equals(mr.getKline())) {
			bqb.must(QueryBuilders.matchPhraseQuery("shootingw", 1));
		}
		if (1 == mr.getShooting51()) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting51", 1));
		}
		if (1 == mr.getShooting52()) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting52", 1));
		}
		if (1 == mr.getShooting53()) {
			bqb.must(QueryBuilders.matchPhraseQuery("shooting53", 1));
		}
		if (mr.getWhiteHors() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("susWhiteHors", 1));// 白马走势(大票)
		}

		if (StringUtils.isNotBlank(mr.getTotalAmt())) {
			bqb.must(QueryBuilders.rangeQuery("dzjy365d").gte(Double.valueOf(mr.getTotalAmt()) * WAN));
		}
		if (StringUtils.isNotBlank(mr.getTotalAmt60d())) {
			bqb.must(QueryBuilders.rangeQuery("dzjy60d").gte(Double.valueOf(mr.getTotalAmt60d()) * WAN));
		}
		if (mr.getZfself() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfself", 1));
		}
		if (mr.getDzjyRct() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("dzjyRct", 1));
		}

		if (mr.getZfbuy() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfbuy", 1));
		}
		if (mr.getZfjj() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfjj", 1));
		}
		if (mr.getCompnayType() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("compnayType", 1));
		}

		if (mr.getQb() == 1) {// 起爆
			bqb.must(QueryBuilders.matchPhraseQuery("qb", 1));
		}
		if (mr.getDibuqixing() == 1) {// 底部旗形
			bqb.must(QueryBuilders.rangeQuery("dibuQixing").gte(1));
		}
		if (mr.getDibuqixing2() == 1) {// 旗形
			bqb.must(QueryBuilders.rangeQuery("dibuQixing2").gte(1));
		}
		if (mr.getZyxing() == 1) {// 中阳带星
			bqb.must(QueryBuilders.rangeQuery("zyxing").gte(1));
		}
		if (mr.getZyxingt() == 1) {// 中阳带星
			bqb.must(QueryBuilders.matchPhraseQuery("zyxingt", 1));
		}
		if (mr.getBreakingVol() == 1) {// 成交量异动
			bqb.must(QueryBuilders.rangeQuery("breakingVol").gte(1));
		}

		if (StringUtils.isNotBlank(mr.getZfStatus())) {
			int t = Integer.valueOf(mr.getZfStatus());
			if (t == 6) {// 证监会核准
				bqb.must(QueryBuilders.matchPhraseQuery("zfStatusDesc", ZfStatus.ZF_ZJHHZ.getDesc()));
			} else if (t == 8) {// 增发中或已完成
				bqb.must(QueryBuilders.rangeQuery("zfStatus").gte(1).lte(2));
			} else {
				bqb.must(QueryBuilders.matchPhraseQuery("zfStatus", t));
			}
		}
		if (mr.getZfObjType() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfObjType", mr.getZfObjType()));
		}
		if (StringUtils.isNotBlank(mr.getZfjjup())) {
			int t = Integer.valueOf(mr.getZfjjup().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfjjup").gte(t));
			}
		}
		if (StringUtils.isNotBlank(mr.getZfjjupStable())) {
			int t = Integer.valueOf(mr.getZfjjupStable().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfjjupStable").gte(t));
			}
		}

		if (StringUtils.isNotBlank(mr.getZfPriceLow())) {
			int t = Integer.valueOf(mr.getZfPriceLow().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfPriceLow").gte(t));
			}
		}
		if (StringUtils.isNotBlank(mr.getHolderNumT3())) {
			double t = Double.valueOf(mr.getHolderNumT3().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("holderNumT3").gte(t));
			}
		}
		if (StringUtils.isNotBlank(mr.getPettm())) {
			double t = Double.valueOf(mr.getPettm().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("pettm").gt(0).lte(t));
			}
		}

		// 增发金额小于等于
		if (StringUtils.isNotBlank(mr.getZfYjAmt())) {
			Long zfYjAmt = Long.valueOf(mr.getZfYjAmt());
			if (zfYjAmt > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfYjAmt").lte(zfYjAmt * 100000000l));
			}
		}
		// 大于等于
		if (StringUtils.isNotBlank(mr.getZfYjAmt2())) {
			Long zfYjAmt = Long.valueOf(mr.getZfYjAmt2());
			if (zfYjAmt > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfYjAmt").gte(zfYjAmt * 100000000l));
			}
		}
		// 股东人数
		if (StringUtils.isNotBlank(mr.getGdrs())) {
			Long lastNum = Long.valueOf(mr.getGdrs());
			if (lastNum > 0) {
				bqb.must(QueryBuilders.rangeQuery("lastNum").gte(1).lte(lastNum));
			}
		}
		// 股东人数变化-- 这里是负数
		if (StringUtils.isNotBlank(mr.getGdrsp())) {
			Double holderNum = Double.valueOf(mr.getGdrsp());
			if (holderNum > 0) {
				bqb.must(QueryBuilders.rangeQuery("holderNum").lte(-holderNum));
			}
		}

		SortOrder order = SortOrder.DESC;
		if (mr.getAsc() == 2) {
			order = SortOrder.ASC;
		}

		String field = "baseGreen";
		int orderBy = mr.getOrderBy();
		if (orderBy == 3) {
			field = "sylttm";
		} else if (orderBy == 6) {
			field = "holderNumP5";
		} else if (orderBy == 7) {
			field = "zfjjup";
		} else if (orderBy == 8) {
			field = "zfPriceLow";
		} else if (orderBy == 9) {
			field = "avgNum";
		} else if (orderBy == 10) {
			field = "dzjy60d";
		} else if (orderBy == 11) {
			field = "dzjy365d";
		} else if (orderBy == 12) {
			field = "dzjyp365d";
		} else if (orderBy == 13) {
			field = "holderNum";
			order = SortOrder.ASC;
		} else if (orderBy == 14) {
			field = "reducZb";
		} else if (orderBy == 15) {
			field = "qixing";
		} else if (orderBy == 16) {
			field = "finDbl";
		}

		FieldSortBuilder sort = SortBuilders.fieldSort(field).unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		NativeSearchQueryBuilder builder = queryBuilder.withQuery(bqb);
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		builder.withPageable(pageable).withSort(sort);
		SearchQuery sq = builder.build();
		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	public Map<String, CodeBaseModel2> getALLForMap() {
		List<CodeBaseModel2> list = getALLForList();
		Map<String, CodeBaseModel2> map = new HashMap<String, CodeBaseModel2>();
		if (list != null) {
			for (CodeBaseModel2 c : list) {
				map.put(c.getCode(), c);
			}
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

	public FinanceBaseInfoHangye getFinanceBaseInfoHangye(String code, int year, int quarter) {
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
}
