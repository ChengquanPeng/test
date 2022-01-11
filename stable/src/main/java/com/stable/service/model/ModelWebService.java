package com.stable.service.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
import com.stable.enums.MonitorType;
import com.stable.enums.SylType;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.EsCodeBaseModelHistDao;
import com.stable.es.dao.base.MonitorPoolDao;
import com.stable.service.ConceptService;
import com.stable.service.StockBasicService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.MonitorPool;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.req.ModelManulReq;
import com.stable.vo.http.req.ModelReq;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ModelWebService {

	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private EsCodeBaseModelHistDao codeBaseModelHistDao;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private MonitorPoolDao monitorPoolDao;

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

	public CodeBaseModelResp getLastOneByCodeResp(String code) {
		return getModelResp(getLastOneByCode2(code));
	}

	public CodeBaseModelResp getHistOneByCodeYearQuarter(String code, int year, int quarter) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("currYear", year));
		bqb.must(QueryBuilders.matchPhraseQuery("currQuarter", quarter));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			CodeBaseModelHist dh = page.getContent().get(0);
			return getModelResp(dh);
		}
		return null;

	}

	public CodeBaseModelResp getHistOneById(String id) {
		log.info("getHistOneById:{}", id);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			CodeBaseModelHist dh = page.getContent().get(0);
			return getModelResp(dh);
		}
		return null;
	}

	private CodeBaseModelResp getModelResp(CodeBaseModel2 dh) {
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
		if (dh.getBaseGreen() == 1) {
			sb1.append("<font color='green'>绿:</font>" + dh.getBaseGreenDesc());
		}
		resp.setBaseInfo(sb1.toString());
		resp.setMonitorDesc(MonitorType.getCodeName(dh.getMonitor()));
		// 收益率
		StringBuffer sb2 = new StringBuffer(SylType.getCodeName(dh.getSylType()));
		sb2.append(Constant.HTML_LINE).append("ttm/jd").append(Constant.HTML_LINE).append(dh.getSylttm()).append("/")
				.append(dh.getSyldjd());
		resp.setSylDesc(sb2.toString());

		// 短线
		StringBuffer sb3 = new StringBuffer("");
		if (dh.getSortChips() == 1) {
			sb3.append("吸筹-收集筹码短线").append(Constant.HTML_LINE);
		}
		if (dh.getSortMode6() == 1) {
			sb3.append("短线6").append(Constant.HTML_LINE);
		}
		if (dh.getSortMode7() == 1) {
			sb3.append("箱体突破").append(Constant.HTML_LINE);
		}
		resp.setSortInfo(sb3.toString());

		// 标签
		StringBuffer tag = new StringBuffer("");
		if (dh.getTagSmallAndBeatf() > 0) {
			tag.append("小而美").append(Constant.HTML_LINE);
		}
		if (dh.getSortChips() > 0) {
			tag.append("吸筹-收集筹码短线").append(Constant.HTML_LINE);
		}
		if (dh.getTagHighZyChance() > 0) {
			tag.append("高质押机会?").append(Constant.HTML_LINE);
		}
		if (dh.getSusBigBoss() == 1) {
			tag.append("疑似大牛").append(Constant.HTML_LINE);
		}
		if (dh.getSusWhiteHors() == 1) {
			tag.append("疑似白马").append(Constant.HTML_LINE);
		}
		resp.setTagInfo(tag.toString());

		// 博弈-基本面
		StringBuffer sb5 = new StringBuffer();
		if (dh.getShooting1() > 0 || dh.getShooting2() > 0 || dh.getShooting3() > 0) {
			sb5.append("<font color='red'>");
			if (dh.getShooting1() > 0) {
				sb5.append("小票底部大宗超5千万,机构代持？非董监高减持");
			}
			if (dh.getShooting2() > 0) {
				sb5.append("大票底部增发超过50亿(越大越好),股东集中，证监会核准-底部拿筹涨停?");
			}
			if (dh.getShooting3() > 0) {
				sb5.append("<a target='_blank' href='https://data.eastmoney.com/rzrq/detail/" + dh.getCode()
						+ ".html'>融资余额暴增?</a>");
			}
			sb5.append("</font>").append(Constant.HTML_LINE);
		}
		// 流通
		sb5.append("流通:").append(dh.getMkv()).append("亿,");
		sb5.append("5%以下:").append(dh.getActMkv()).append("亿,");
		if (dh.getZfjjup() > 0) {
			sb5.append(dh.getZfjjup());
			if (dh.getZfjjupStable() > 0) {
				sb5.append("<font color='red'>/stable").append(dh.getZfjjupStable()).append("</font>");
			}
			sb5.append("年未大涨,");
		}
		if (dh.getBousOK() == 1) {
			sb5.append("近5年业绩不亏,");
		}
		if (dh.getFinOK() == 1) {
			sb5.append("近5年分红,");
		}
		sb5.append(Constant.HTML_LINE);
		sb5.append("前3大股东:").append(dh.getHolderNumT3()).append("%");
		sb5.append(",股东人数(少):").append(CurrencyUitl.covertToString(dh.getLastNum()));
		sb5.append(",人均持股(高):").append(CurrencyUitl.covertToString(dh.getAvgNum()));
		sb5.append(",变化:").append(dh.getHolderNum()).append("%");
		sb5.append(Constant.HTML_LINE);
		// 博弈-增发
		if (dh.getZfStatus() == 1 || dh.getZfStatus() == 2) {
			if (dh.getZfStatus() == 1) {
				sb5.append("<font color='red'>");
			} else {
				sb5.append("<font color='green'>");
			}
			sb5.append("增发进度" + (dh.getCompnayType() == 1 ? "(国资)" : "") + ":" + dh.getZfStatusDesc());
			sb5.append("</font>");

			if (dh.getZfStatus() == 1) {
				if (dh.getZfYjAmt() > 0) {
					sb5.append(",预增发金额:").append(CurrencyUitl.covertToString(dh.getZfYjAmt()));
				}
			} else {
				if (dh.getZfYjAmt() > 0) {
					sb5.append(",增发金额:").append(CurrencyUitl.covertToString(dh.getZfYjAmt()));
				}
				sb5.append(",增发价格:").append(dh.getZfPrice());
			}

		}
		// 最近一次增发
		if (dh.getZflastOkDate() > 0) {
			sb5.append(",实施日期:").append(dh.getZflastOkDate()).append(",");
			if (dh.getZfself() == 1) {
				sb5.append("<font color='green'>底部增发</font>,");
			}
			if (dh.getZfPriceLow() > 0) {
				sb5.append("<font color='red'>低于增发价:").append(dh.getZfPriceLow()).append("%</font>,");
			}
			if (dh.getGsz() == 1) {
				sb5.append("3年内有高送转,");
			}
			if (dh.getZfObjType() == 1) {
				sb5.append("6个月,");
			} else if (dh.getZfObjType() == 2) {
				sb5.append("混合(6月+大股东),");
			} else if (dh.getZfObjType() == 3) {
				sb5.append("大股东,");
			} else if (dh.getZfObjType() == 4) {
				sb5.append("其他,");
			}
		}
		// 解禁
		if (dh.getZfjj() == 1) {
			sb5.append("增发解禁(" + dh.getZfjjDate() + ")").append(Constant.HTML_LINE);
		}
		sb5.append(Constant.HTML_LINE);
		// 大宗
		if (dh.getDzjyRct() > 0) {
			sb5.append(",1年大宗超亿(均价:").append(dh.getDzjyAvgPrice()).append(")");
			if (dh.getTagDzPriceLow() > 0) {
				sb5.append(",低于均价:").append(dh.getTagDzPriceLow()).append("%");
			}
		}
		if (dh.getDzjy60d() > 0) {
			sb5.append(",2个月大宗:").append(dh.getDzjy60d()).append("万");
		}
		sb5.append(Constant.HTML_LINE);

		// 是否确定
		if (dh.getPls() == 0) {
			sb5.append("人工: 未确定").append(Constant.HTML_LINE);
		} else if (dh.getPls() == 1) {
			sb5.append("人工: 已确定").append(Constant.HTML_LINE);
		} else if (dh.getPls() == 2) {
			sb5.append("人工: 排除").append(Constant.HTML_LINE);
		}
		resp.setZfjjInfo(sb5.toString());
//		resp.setIncomeShow(dh.getCurrIncomeTbzz() + "%");
//		if (dh.getForestallIncomeTbzz() > 0) {
//			resp.setIncomeShow(resp.getIncomeShow() + "(" + dh.getForestallIncomeTbzz() + "%)");
//		}
//		resp.setProfitShow(dh.getCurrProfitTbzz() + "%");
//		if (dh.getForestallIncomeTbzz() > 0) {
//			resp.setProfitShow(resp.getProfitShow() + "(" + dh.getForestallProfitTbzz() + "%)");
//		}
		return resp;
	}

	public List<CodeBaseModelResp> getListForWeb(ModelReq mr, EsQueryPageReq querypage) {
		log.info("CodeBaseModel getListForWeb mr={}", mr);

		List<CodeBaseModel2> list = getList(mr, querypage);
		List<CodeBaseModelResp> res = new LinkedList<CodeBaseModelResp>();
		if (list != null) {
			for (CodeBaseModel2 dh : list) {
				res.add(getModelResp(dh));
			}
		}
		return res;
	}

	public List<String> listCodeByCodeConceptName(String conceptName) {
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

	public void addPlsManual(ModelManulReq req) {
		String code = req.getCode();
		int pls = req.getPls();
		int timemonth = req.getTimemonth();
		if (pls != 1 && pls != 2) {
			throw new RuntimeException("i != 1 && i != 2 ? ");
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
		if (date != 1) {
			String remark = req.getBuyRea() + " " + req.getSoldRea();
			CodeBaseModel2 c = getLastOneByCode2(code);
			BeanCopy.copy(req, c);
			c.setPls(pls);
			c.setPlst(date);
			c.setLstmt(DateUtil.getTodayIntYYYYMMDD());
			c.setBuyRea(remark);
			codeBaseModel2Dao.save(c);

			MonitorPool pool = monitorPoolService.getMonitorPool(code);
			if (pls == 2) {// 2不在池子
				pool.setMonitor(MonitorType.NO.getCode());
				pool.setUpTodayChange(0);
				pool.setRealtime(0);
				pool.setOffline(0);
				pool.setDzjy(0);
				pool.setHolderNum(0);
				pool.setYkb(0);
				pool.setZfdone(0);
				pool.setRemark("");
				pool.setListenerGg(0);
				pool.setBuyLowVol(30);
				monitorPoolDao.save(pool);
			} else if (pls == 1 && c.getPls() != 1) {// 1不在池子，且原来不等于1
				pool.setMonitor(MonitorType.MANUAL.getCode());
				pool.setUpTodayChange(3);
				pool.setRealtime(1);
				int dt = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
				pool.setDzjy(dt);
				pool.setHolderNum(dt);
				pool.setYkb(1);
				pool.setZfdone(1);
				pool.setRemark(remark);
				pool.setListenerGg(req.getListenerGg());
				pool.setBuyLowVol(0);
				monitorPoolDao.save(pool);
			}
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
			List<String> list = conceptService.listCodesByAliasCode(mr.getConceptId());
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			} else {
				return new LinkedList<CodeBaseModel2>();
			}
		} else if (StringUtils.isNotBlank(mr.getConceptName())) {
			List<String> list = listCodeByCodeConceptName(mr.getConceptName());
			if (list.size() > 0) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			} else {
				return new LinkedList<CodeBaseModel2>();
			}
		}

		String field = "baseGreen";

		int orderBy = mr.getOrderBy();
		if (orderBy == 2) {
			field = "zflastOkDate";
		} else if (orderBy == 3) {
			field = "sylttm";
		} else if (orderBy == 4) {
			field = "syl";
		} else if (orderBy == 5) {
			field = "sylType";
		} else if (orderBy == 6) {
			field = "holderNumP5";
		} else if (orderBy == 7) {
			field = "zfjjup";
		} else if (orderBy == 8) {
			field = "zfPriceLow";
		} else if (orderBy == 9) {
			field = "avgNum";
		}

		if (StringUtils.isNotBlank(mr.getMonitor())) {
			int m = Integer.valueOf(mr.getMonitor());
			if (m == 9999) {
				bqb.must(QueryBuilders.rangeQuery("monitor").gte(1));
			} else {
				bqb.must(QueryBuilders.matchPhraseQuery("monitor", m));
			}
		}
		if (mr.getPls() != -1) {
			bqb.must(QueryBuilders.matchPhraseQuery("pls", Integer.valueOf(mr.getPls())));
		}
		if (StringUtils.isNotBlank(mr.getBred())) {
			bqb.must(QueryBuilders.matchPhraseQuery("baseRed", Integer.valueOf(mr.getBred())));
		}
		if (StringUtils.isNotBlank(mr.getByellow())) {
			bqb.must(QueryBuilders.matchPhraseQuery("baseYellow", Integer.valueOf(mr.getByellow())));
		}
//		if (StringUtils.isNotBlank(mr.getBblue())) {
//			bqb.must(QueryBuilders.matchPhraseQuery("baseBlue", Integer.valueOf(mr.getBblue())));
//		}
//		if (StringUtils.isNotBlank(mr.getBgreen())) {
//			bqb.must(QueryBuilders.matchPhraseQuery("baseGreen", Integer.valueOf(mr.getBgreen())));
//		}
		if (StringUtils.isNotBlank(mr.getBsyl())) {
			bqb.must(QueryBuilders.matchPhraseQuery("sylType", Integer.valueOf(mr.getBsyl())));
		}
		if (StringUtils.isNotBlank(mr.getMkv())) {
			double mkv = Double.valueOf(mr.getMkv());
			if (mkv > 0) {
				bqb.must(QueryBuilders.rangeQuery("mkv").lte(mkv));
			}
		}
		if (mr.getTagIndex() > 0) {
			if (mr.getTagIndex() == 1) {
				bqb.must(QueryBuilders.matchPhraseQuery("sortChips", 1));// 吸筹-收集筹码短线
			} else if (mr.getTagIndex() == 2) {
				bqb.must(QueryBuilders.matchPhraseQuery("susBigBoss", 1));// 交易面疑似大牛
			} else if (mr.getTagIndex() == 3) {
				bqb.must(QueryBuilders.matchPhraseQuery("susWhiteHors", 1));// 交易面疑似白马
			} else if (mr.getTagIndex() == 5) {
				bqb.must(QueryBuilders.matchPhraseQuery("tagSmallAndBeatf", 1));// 小而美
			} else if (mr.getTagIndex() == 4) {
				bqb.must(QueryBuilders.matchPhraseQuery("tagHighZyChance", 1));// 高质押机会
			}
		}
		if (mr.getShooting() > 0) {
			if (mr.getShooting() == 1) {
				bqb.must(QueryBuilders.matchPhraseQuery("shooting1", 1));
			} else if (mr.getShooting() == 2) {
				bqb.must(QueryBuilders.matchPhraseQuery("shooting2", 1));
			} else if (mr.getShooting() == 3) {
				bqb.must(QueryBuilders.matchPhraseQuery("shooting3", 1));
			}
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

		if (mr.getSort6() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("sortMode6", 1));
		}
		if (mr.getSort7() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("sortMode7", 1));
		}
		if (mr.getZfjj() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfjj", 1));
		}
		if (mr.getCompnayType() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("compnayType", 1));
		}
		if (mr.getFinOK() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("finOK", 1));
		}
		if (mr.getBousOK() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("bousOK", 1));
		}

		if (StringUtils.isNotBlank(mr.getZfStatus())) {
			int t = Integer.valueOf(mr.getZfStatus());
			if (t == 6) {// 证监会核准
				bqb.must(QueryBuilders.matchPhraseQuery("zfStatusDesc", CodeModelService.ZF_ZJHHZ));
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

		// 增发金额小于等于
		if (StringUtils.isNotBlank(mr.getZfYjAmt())) {
			Long zfYjAmt = Long.valueOf(mr.getZfYjAmt());
			if (zfYjAmt > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfYjAmt").lte(zfYjAmt * 100000000l));
			}
		}

		SortOrder order = SortOrder.DESC;
		if (mr.getAsc() == 2) {
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

}
