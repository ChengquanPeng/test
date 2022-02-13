package com.stable.service.monitor;

import java.util.Comparator;
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
import com.stable.es.dao.base.MonitorPoolDao;
import com.stable.service.ChipsService;
import com.stable.service.ChipsZfService;
import com.stable.service.ConceptService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.model.ModelWebService;
import com.stable.spider.ths.ThsBonusSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BonusHist;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.MonitorPool;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaSummary;
import com.stable.vo.http.resp.MonitorPoolResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 监听池
 */
@Service
@Log4j2
public class MonitorPoolService {
	@Autowired
	private MonitorPoolDao monitorPoolDao;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private ModelWebService modelWebService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private ThsBonusSpider thsBonusSpider;
	@Autowired
	private ChipsZfService chipsZfService;
	@Autowired
	private ChipsService chipsService;

	// 移除监听
	public void delMonit(String code, String remark) {
		MonitorPool c = getMonitorPool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(0);
		c.setUpPrice(0);
		c.setDownPrice(0);
		c.setUpTodayChange(0);
		c.setDownTodayChange(0);
		c.setRealtime(0);
		c.setOffline(0);
		c.setBuyLowVol(0);
		c.setHolderNum(0);
		c.setXjl(0);
		c.setDzjy(0);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + c.getUpdateDate());
		}
		monitorPoolDao.save(c);
		updateBaseMoniStatus(code, c.getMonitor(), c.getRemark());
	}
	
	public void saveOrUpdate(MonitorPool mp) {
		monitorPoolDao.save(mp);
	}

	private void updateBaseMoniStatus(String code, int monitor, String buyRea) {
		CodeBaseModel2 cbm = modelWebService.getLastOneByCode2(code);
//		cbm.setMonitor(monitor);
		cbm.setBuyRea(buyRea);// 同步-备注
		codeBaseModel2Dao.save(cbm);
	}

	// 加入监听
	public void addMonitor(String code, int monitor, int realtime, int offline, double upPrice, double downPrice,
			double upTodayChange, double downTodayChange, String remark, int ykb, int zfdone, int holderNum,
			int buyLowVol, int xjl, int dzjy, int listenerGg) {
		if (monitor <= 0) {
			throw new RuntimeException("monitor<=0 ?");
		}
//		if (realtime == 0 && offline == 0) {
//			throw new RuntimeException("realtime == 0 && offline == 0 ?");
//		}
//		if (upPrice == 0 && upTodayChange == 0 && downPrice == 0 && downTodayChange == 0) {
//			throw new RuntimeException(
//					"upPrice == 0 && upTodayChange == 0 && downPrice == 0 && downTodayChange == 0 ?");
//		}
		MonitorPool c = getMonitorPool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(monitor);
		c.setRealtime(realtime);
		c.setOffline(offline);
		c.setDownPrice(downPrice);
		c.setDownTodayChange(downTodayChange);
		c.setUpPrice(upPrice);
		c.setUpTodayChange(upTodayChange);
		c.setZfdone(zfdone);
		c.setBuyLowVol(buyLowVol);
		c.setXjl(xjl);

		c.setListenerGg(listenerGg);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + " " + c.getUpdateDate());
		}
		int dt = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
		if (ykb > 0) {
			c.setYkb(dt);
		} else {
			c.setYkb(0);
		}
		if (holderNum > 0) {
			c.setHolderNum(dt);
		} else {
			c.setHolderNum(0);
		}
		if (dzjy > 0) {
			c.setDzjy(dt);
		} else {
			c.setDzjy(0);
		}
		monitorPoolDao.save(c);
		updateBaseMoniStatus(code, c.getMonitor(), c.getRemark());
	}

	public MonitorPool getMonitorPool(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		SearchQuery sq = queryBuilder.build();
		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		MonitorPool cp = new MonitorPool();
		cp.setCode(code);
		return cp;
	}

	// 所有监听池
	public List<MonitorPool> getMonitorPool() {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		if (pageable != null) {
			queryBuilder = queryBuilder.withPageable(pageable);
		}
		SearchQuery sq = queryBuilder.build();

		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public Map<String, MonitorPool> getMonitorPoolMap() {
		return getPoolMap(this.getMonitorPool());
	}

	public Map<String, MonitorPool> getPoolMap(List<MonitorPool> list) {
		Map<String, MonitorPool> map = new HashMap<String, MonitorPool>();
		if (list != null) {
			for (MonitorPool c : list) {
				map.put(c.getCode(), c);
			}
		}
		return map;
	}

	public Map<String, TradeHistInfoDaliyNofq> getPoolMap2(List<TradeHistInfoDaliyNofq> list) {
		Map<String, TradeHistInfoDaliyNofq> map = new HashMap<String, TradeHistInfoDaliyNofq>();
		if (list != null) {
			for (TradeHistInfoDaliyNofq c : list) {
				map.put(c.getCode(), c);
			}
		}
		return map;
	}

	/**
	 * 监听列表-实时
	 */
	public List<MonitorPool> getPoolListForMonitor(int realtime, int offline) {
		int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
		int size = EsQueryPageUtil.queryPage9999.getPageSize();
		log.info("queryPage pageNum={},size={}", pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		// 监听列表 should OR 或 查询
		bqb.must(QueryBuilders.rangeQuery("monitor").gt(0));
		if (realtime > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("realtime", 1));
		}
		if (offline > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("offline", 1));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<MonitorPoolResp> getListForWeb(String code, int monitor, int monitoreq, EsQueryPageReq querypage,
			String aliasCode) {
		log.info("CodeBaseModel getListForWeb code={},num={},size={},aliasCode={},monitor={},monitoreq={}", code,
				querypage.getPageNum(), querypage.getPageSize(), aliasCode, monitor, monitoreq);

		List<MonitorPool> list = getList(code, monitor, monitoreq, 0, 0, querypage, aliasCode, 0, 0, 0);
		List<MonitorPoolResp> res = new LinkedList<MonitorPoolResp>();
		if (list != null) {
			for (MonitorPool dh : list) {
				MonitorPoolResp resp = new MonitorPoolResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setMonitorDesc(MonitorType.getCodeName(dh.getMonitor()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<MonitorPool> getList(String code, int monitor, int monitoreq, int ykb, int zfdone,
			EsQueryPageReq querypage, String aliasCode, int holderNum, int buyLowVol, int xjl) {
		return getList(code, monitor, monitoreq, ykb, zfdone, querypage, aliasCode, holderNum, buyLowVol, xjl, 0);
	}

	public List<MonitorPool> getList(String code, int monitor, int monitoreq, int ykb, int zfdone,
			EsQueryPageReq querypage, String aliasCode, int holderNum, int buyLowVol, int xjl, int dzjy) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else if (StringUtils.isNotBlank(aliasCode)) {
			List<String> list = this.conceptService.listCodesByAliasCode(aliasCode, EsQueryPageUtil.queryPage9999);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		}
		if (monitor > 0) {
			bqb.must(QueryBuilders.rangeQuery("monitor").gt(0));
		}
		if (ykb > 0) {
			bqb.must(QueryBuilders.rangeQuery("ykb").gt(0));
		}
		if (holderNum > 0) {
			bqb.must(QueryBuilders.rangeQuery("holderNum").gt(0));
		}
		if (buyLowVol > 0) {
			bqb.must(QueryBuilders.rangeQuery("buyLowVol").gt(0));
		}
		if (xjl > 0) {
			bqb.must(QueryBuilders.rangeQuery("xjl").gt(0));
		}
		if (monitoreq > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("monitor", monitoreq));
		}
		if (zfdone > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfdone", zfdone));
		}
		if (dzjy > 0) {
			bqb.must(QueryBuilders.rangeQuery("dzjy").gt(0));
		}

		FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	// 完成定增预警
	public void jobZfDoneWarning() {
		List<MonitorPool> list = getList("", 0, 0, 0, 1, EsQueryPageUtil.queryPage9999, "", 0, 0, 0);
		if (list != null) {
			int oneYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -370));
			int sysdate = DateUtil.getTodayIntYYYYMMDD();
			List<ZengFaDetail> zfdl = new LinkedList<ZengFaDetail>();
			List<ZengFaSummary> zfsl = new LinkedList<ZengFaSummary>();
			List<FenHong> fhl = new LinkedList<FenHong>();
			List<BonusHist> bhl = new LinkedList<BonusHist>();
			List<ZengFa> zfl = new LinkedList<ZengFa>();
			// 抓包
			for (MonitorPool mp : list) {
				thsBonusSpider.dofetchBonusInner(sysdate, mp.getCode(), zfdl, zfsl, fhl, bhl, zfl, 0);
			}
			thsBonusSpider.saveAll(zfdl, zfsl, fhl, bhl);
			// 预警
			for (MonitorPool mp : list) {
				ZengFa zf = chipsZfService.getLastZengFa(mp.getCode(), ZfStatus.ING.getCode());
				if (!chipsZfService.isZfDateOk(zf, oneYearAgo)) {
					mp.setZfdone(0);
					mp.setZfdoneZjh(0);
					monitorPoolDao.save(mp);
					WxPushUtil
							.pushSystem1(stockBasicService.getCodeName2(mp.getCode()) + " 已完成增发,备注:" + mp.getRemark());
				} else {
					if (mp.getZfdoneZjh() == 0 && zf != null
							&& ZfStatus.ZF_ZJHHZ.getDesc().equals(zf.getStatusDesc())) {
						mp.setZfdoneZjh(1);
						WxPushUtil.pushSystem1(
								stockBasicService.getCodeName2(mp.getCode()) + " 增发已通过证监会核准！ 备注:" + mp.getRemark());
					}
				}
			}
		}
	}

	// 股东人数预警
	public List<MonitorPool> getHolderWarningList() {
		return getList("", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 1, 0, 0);
	}

	public void jobHolderWarning() {
		log.info("股东人数预警");
		List<MonitorPool> list = getHolderWarningList();
		if (list != null) {
			// 预警
			for (MonitorPool mp : list) {
				List<HolderNum> hml = chipsService.getHolderNumList45(mp.getCode());
				if (hml != null && hml.size() > 1) {
					HolderNum hn0 = hml.get(0);
					if (hn0.getDate() >= mp.getHolderNum()) {
						boolean islow = hml.get(1).getNum() > hn0.getNum();
						WxPushUtil.pushSystem1(stockBasicService.getCodeName2(mp.getCode()) + " 股东人数:"
								+ (islow ? "下降" : "上涨") + ",备注:" + mp.getRemark());
						mp.setHolderNum(DateUtil.getTodayIntYYYYMMDD());
						monitorPoolDao.save(mp);
					}
				}
			}
		}
	}

	// 买点:地量
	public void jobBuyLowVolWarning() {
		List<MonitorPool> list = getList("", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 0, 1, 0);
		if (list != null) {
			Integer today = DateUtil.getTodayIntYYYYMMDD();
			for (MonitorPool mp : list) {
				EsQueryPageReq req = new EsQueryPageReq(mp.getBuyLowVol());
				List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(mp.getCode(), 0,
						today, req, SortOrder.DESC);
				TradeHistInfoDaliyNofq tday = l2.get(0);
				double l = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getVolume)).get()
						.getVolume();
				double factor = CurrencyUitl.topPriceN(l, 1.03);
				if (tday.getVolume() <= factor) {
					WxPushUtil.pushSystem1(
							stockBasicService.getCodeName2(mp.getCode()) + " 差不多已经地量(" + mp.getBuyLowVol() + "交易日),日期从"
									+ l2.get(l2.size() - 1).getDate() + " " + tday.getDate() + ",备注:" + mp.getRemark());
				}
			}
		}
	}

	// 大宗交易
	public void jobDzjyWarning() {
		ThreadsUtil.sleepRandomSecBetween15And30();
		List<MonitorPool> list = getList("", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 0, 0, 0, 1);
		List<String> l = new LinkedList<String>();
		if (list != null) {
			Integer today = DateUtil.getTodayIntYYYYMMDD();
			for (MonitorPool mp : list) {
				Dzjy dzjy = chipsService.getLastDzjy(mp.getCode());
				if (dzjy.getDate() > mp.getDzjy()) {
					l.add(mp.getCode());
					mp.setDzjy(today);
					monitorPoolDao.save(mp);
				}
			}
			if (l.size() > 0) {
				StringBuffer sb = new StringBuffer();
				for (String s : l) {
					sb.append(stockBasicService.getCodeName2(s)).append(Constant.DOU_HAO);
				}
				WxPushUtil.pushSystem1("关注票的大宗交易:" + sb.toString());
			}
		}

	}

//	/**
//	 * 1.短期有9.5%的涨幅
//	 */
//	private boolean isTodayPriceOk(String code, int date) {
//		// 3个月新高，22*3=60
//		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
//				EsQueryPageUtil.queryPage30, SortOrder.DESC);
//		for (TradeHistInfoDaliyNofq r : l2) {
//			if (r.getTodayChangeRate() >= 9.5) {
//				return true;
//			}
//		}
//		log.info("{} 最近30个工作日无大涨交易", code);
//		return false;
//	}
//
//	/**
//	 * 2.均线
//	 */
//	private boolean isWhiteHorseForSortV5(String code, int date) {
//		EsQueryPageReq req = EsQueryPageUtil.queryPage10;
//		List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLast60(code, date, req, true);
//		int c = 0;
//		for (StockAvgBase sa : clist30) {
//			if (sa.getAvgPriceIndex30() >= sa.getAvgPriceIndex60()
//					&& sa.getAvgPriceIndex20() >= sa.getAvgPriceIndex30()) {
//				c++;
//			}
//		}
//		if (c >= 8) {
//			return true;
//		}
//		return false;
//	}
}
