package com.stable.service.monitor;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.stable.es.dao.base.MonitorPoolUserDao;
import com.stable.service.ChipsService;
import com.stable.service.ChipsZfService;
import com.stable.service.ConceptService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.FinanceService;
import com.stable.service.MyUrlService;
import com.stable.service.StockBasicService;
import com.stable.service.model.WebModelService;
import com.stable.service.model.prd.UserService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.spider.ths.ThsAnnSpider;
import com.stable.spider.ths.ThsBonusSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.MonitoringUitl;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.BonusHist;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.bus.UserInfo;
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
	private MonitorPoolUserDao monitorPoolDao;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private WebModelService modelWebService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private ThsBonusSpider thsBonusSpider;
	@Autowired
	private ChipsZfService chipsZfService;
	@Autowired
	private ChipsService chipsService;
	@Autowired
	private UserService userService;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private MyUrlService myUrlService;

//	@javax.annotation.PostConstruct
//	public void init() {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
//				int size = EsQueryPageUtil.queryPage9999.getPageSize();
//				log.info("queryPage pageNum={},size={}", pageNum, size);
//				Pageable pageable = PageRequest.of(pageNum, size);
//				BoolQueryBuilder bqb = QueryBuilders.boolQuery();
//				bqb.must(QueryBuilders.matchPhraseQuery("userId", 0));
//				NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
//				SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();
//
//				Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
//				if (page != null && !page.isEmpty()) {
//					monitorPoolDao.deleteAll(page.getContent());
//				}
//				log.info("done.init");
//			}
//		}).start();
//	}

	public String getId(long userId, String code) {
		if (userId < Constant.MY_ID) {
			throw new RuntimeException("错误的userId");
		}
		return userId + code;
	}

	// 移除监听
	public void delMonit(long userId, String code, String remark) {
		MonitorPoolTemp c = getMonitorPoolById(userId, code);
		c.setMonitor(0);
		c.setUpPrice(0);
		c.setDownPrice(0);
		c.setUpTodayChange(0);
		c.setDownTodayChange(0);
		c.setRealtime(0);
		c.setOffline(0);
		c.setBuyLowVol(0);
		c.setHolderNum(0);
//		c.setXjl(0);
		c.setDzjy(0);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark);
		}
		toSave(c);
		updateBaseMoniStatus(userId, code, c.getRemark());
	}

	private void updateBaseMoniStatus(long userId, String code, String buyRea) {
		if (userId == Constant.MY_ID) {
			CodeBaseModel2 cbm = modelWebService.getLastOneByCode2(code);
			cbm.setBuyRea(buyRea);// 同步-备注
			codeBaseModel2Dao.save(cbm);
		}
	}

	public void reset(MonitorPoolTemp pool) {
		pool.setMonitor(MonitorType.NO.getCode());
		pool.setUpTodayChange(0);
		pool.setDownTodayChange(0);
		pool.setRealtime(0);
		pool.setOffline(0);
		pool.setDzjy(0);
		pool.setHolderNum(0);
		pool.setYkb(0);
		pool.setZfdone(0);
		pool.setZfdoneZjh(0);
		pool.setListenerGg(0);// 监听公告
		pool.setBuyLowVol(0);
		pool.setUpPrice(0);
		pool.setDownPrice(0);
		pool.setRgqbPrice(0);
	}

	// 加入监听
	public void addMonitor(long userId, String code, int monitor, int realtime, int offline, double upPrice,
			double downPrice, double upTodayChange, double downTodayChange, String remark, int ykb, int zfdone,
			int holderNum, int buyLowVol, int xjl, int dzjy, int listenerGg, double rgqbPrice) {
		MonitorPoolTemp c = getMonitorPoolById(userId, code);
		if (rgqbPrice > 0) {// 设置
			c.setRgqbPrice(rgqbPrice);
			toSave(c);
			return;
		} else if (rgqbPrice <= 0 && c.getRgqbPrice() > 0) {// 归0
			c.setRgqbPrice(0);
			toSave(c);
			return;
		}

		if (monitor <= 0) {
			throw new RuntimeException("monitor<=0 ?");
		}
		c.setMonitor(monitor);
		c.setRealtime(realtime);
		c.setOffline(offline);
		c.setDownPrice(downPrice);
		c.setDownTodayChange(downTodayChange);
		c.setUpPrice(upPrice);
		c.setUpTodayChange(upTodayChange);
		c.setZfdone(zfdone);
		c.setBuyLowVol(buyLowVol);
//		c.setXjl(xjl);
//		c.setShotPointCheck(shotPointCheck);
		c.setListenerGg(listenerGg);

//		if (StringUtils.isBlank(remark)) {
//			c.setRemark("");
//		} else {
//			c.setRemark(remark);
//		}
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

		if (userId != Constant.MY_ID) {// 默认不监听
//			c.setShotPointCheck(0);
			c.setListenerGg(0);
		}
		toSave(c);
//		updateBaseMoniStatus(userId, code, c.getRemark()); 统一在人工页维护
	}

	public MonitorPoolTemp getMonitorPoolById(long userId, String code) {
		String id = getId(userId, code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		SearchQuery sq = queryBuilder.build();
		Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		MonitorPoolTemp cp = new MonitorPoolTemp();
		cp.setCode(code);
		cp.setId(id);
		cp.setUserId(userId);
		return cp;
	}

	// 所有监听池
	public List<MonitorPoolTemp> getMonitorPool(long userId, boolean allListing) {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		bqb.must(QueryBuilders.matchPhraseQuery("userId", userId));
		if (allListing) {
			bqb.must(QueryBuilders.rangeQuery("monitor").gt(0));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		if (pageable != null) {
			queryBuilder = queryBuilder.withPageable(pageable);
		}
		SearchQuery sq = queryBuilder.build();

		Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public Map<String, MonitorPoolTemp> getMonitorPoolMap() {
		return getPoolMap(this.getMonitorPool(Constant.MY_ID, false));
	}

	public Map<String, MonitorPoolTemp> getPoolMap(List<MonitorPoolTemp> list) {
		Map<String, MonitorPoolTemp> map = new HashMap<String, MonitorPoolTemp>();
		if (list != null) {
			for (MonitorPoolTemp c : list) {
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

	// 查询My的起爆点
	public Set<MonitorPoolTemp> getMyQibao() {
		int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
		int size = EsQueryPageUtil.queryPage9999.getPageSize();
		log.info("queryPage pageNum={},size={}", pageNum, size);

		Set<MonitorPoolTemp> listall = new HashSet<MonitorPoolTemp>();

		// 1.旗形
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("userId", Constant.MY_ID));
		bqb.must(QueryBuilders.rangeQuery("shotPointPrice").gt(0));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();
		Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			List<MonitorPoolTemp> t1 = page.getContent();
			listall.addAll(t1);
		}

		// 2.十字星
		BoolQueryBuilder bqb21 = QueryBuilders.boolQuery();
		bqb21.must(QueryBuilders.matchPhraseQuery("userId", Constant.MY_ID));
		bqb21.must(QueryBuilders.rangeQuery("shotPointPriceSzx").gt(0));
		NativeSearchQueryBuilder queryBuilder21 = new NativeSearchQueryBuilder();
		SearchQuery sq21 = queryBuilder21.withQuery(bqb21).withPageable(pageable).build();
		Page<MonitorPoolTemp> page21 = monitorPoolDao.search(sq21);
		if (page21 != null && !page21.isEmpty()) {
			List<MonitorPoolTemp> t2 = page21.getContent();
			listall.addAll(t2);
		}
		// 4.洗盘起爆
		BoolQueryBuilder bqb4 = QueryBuilders.boolQuery();
		bqb4.must(QueryBuilders.matchPhraseQuery("userId", Constant.MY_ID));
		bqb4.must(QueryBuilders.rangeQuery("xpPrice").gt(0));
		NativeSearchQueryBuilder queryBuilder4 = new NativeSearchQueryBuilder();
		SearchQuery sq4 = queryBuilder4.withQuery(bqb4).withPageable(pageable).build();
		Page<MonitorPoolTemp> page4 = monitorPoolDao.search(sq4);
		if (page4 != null && !page4.isEmpty()) {
			List<MonitorPoolTemp> t4 = page4.getContent();
			listall.addAll(t4);
		}

		// 3.人工起爆
		BoolQueryBuilder bqb2 = QueryBuilders.boolQuery();
		bqb2.must(QueryBuilders.matchPhraseQuery("userId", Constant.MY_ID));
		bqb2.must(QueryBuilders.rangeQuery("rgqbPrice").gt(0));
		NativeSearchQueryBuilder queryBuilder2 = new NativeSearchQueryBuilder();
		SearchQuery sq2 = queryBuilder2.withQuery(bqb2).withPageable(pageable).build();
		Page<MonitorPoolTemp> page2 = monitorPoolDao.search(sq2);
		if (page2 != null && !page2.isEmpty()) {
			List<MonitorPoolTemp> t2 = page2.getContent();
			listall.addAll(t2);
		}
		return listall;
	}

	/**
	 * 监听列表-实时
	 */
	public List<MonitorPoolTemp> getPoolListForMonitor(long userId, int realtime, int offline, boolean sort1) {
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
		if (userId > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("userId", userId));
		}
		if (sort1) {// 熊市开关
			bqb.mustNot(QueryBuilders.matchPhraseQuery("monitor", MonitorType.SORT1.getCode()));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<MonitorPoolResp> getListForWeb(long userId, String code, int monitor, int monitoreq,
			EsQueryPageReq querypage, String aliasCode) {
		log.info("CodeBaseModel getListForWeb code={},num={},size={},aliasCode={},monitor={},monitoreq={}", code,
				querypage.getPageNum(), querypage.getPageSize(), aliasCode, monitor, monitoreq);
		List<MonitorPoolTemp> list = getList(userId, code, monitor, monitoreq, 0, 0, querypage, aliasCode, 0, 0, 0);
		List<MonitorPoolResp> res = new LinkedList<MonitorPoolResp>();
		if (list != null) {
			for (MonitorPoolTemp dh : list) {
				MonitorPoolResp resp = new MonitorPoolResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setMonitorDesc(MonitorType.getCodeName(dh.getMonitor()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<MonitorPoolTemp> getList(long userId, String code, int monitor, int monitoreq, int ykb, int zfdone,
			EsQueryPageReq querypage, String aliasCode, int holderNum, int buyLowVol, int xjl) {
		return getList(userId, code, monitor, monitoreq, ykb, zfdone, querypage, aliasCode, holderNum, buyLowVol, xjl,
				0, 0);
	}

	public List<MonitorPoolTemp> getList(long userId, String code, int monitor, int monitoreq, int ykb, int zfdone,
			EsQueryPageReq querypage, String aliasCode, int holderNum, int buyLowVol, int xjl, int dzjy,
			int listenerGg) {
		if (userId < Constant.MY_ID) {
			throw new RuntimeException("错误的userId");
		}
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("userId", userId));
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
		if (listenerGg > 0) {
			bqb.must(QueryBuilders.rangeQuery("listenerGg").gt(0));
		}

		FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	// 完成定增预警
	public void jobZfDoneWarning() {
		List<UserInfo> ulist = userService.getUserListForMonitorS1();
		for (UserInfo u : ulist) {
			List<MonitorPoolTemp> list = getList(u.getId(), "", 0, 0, 0, 1, EsQueryPageUtil.queryPage9999, "", 0, 0, 0);
			if (list != null) {
				int oneYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -370));
				int sysdate = DateUtil.getTodayIntYYYYMMDD();
				List<ZengFaDetail> zfdl = new LinkedList<ZengFaDetail>();
				List<ZengFaSummary> zfsl = new LinkedList<ZengFaSummary>();
				List<FenHong> fhl = new LinkedList<FenHong>();
				List<BonusHist> bhl = new LinkedList<BonusHist>();
				List<ZengFa> zfl = new LinkedList<ZengFa>();
				// 抓包
				for (MonitorPoolTemp mp : list) {
					thsBonusSpider.dofetchBonusInner(sysdate, mp.getCode(), zfdl, zfsl, fhl, bhl, zfl, 0, null);
				}
				thsBonusSpider.saveAll(zfdl, zfsl, fhl, bhl);
				// 预警
				for (MonitorPoolTemp mp : list) {
					ZengFa zf = chipsZfService.getLastZengFa(mp.getCode(), ZfStatus.ING.getCode());
					if (!chipsZfService.isZfDateOk(zf, oneYearAgo)) {
						mp.setZfdone(0);
						mp.setZfdoneZjh(0);
						toSave(mp);
						MsgPushServer.pushTextToUser(stockBasicService.getCodeName2(mp.getCode()) + " 已完成增发",
								"备注:" + mp.getRemark(), u);
					} else {
						if (mp.getZfdoneZjh() == 0 && zf != null
								&& ZfStatus.ZF_ZJHHZ.getDesc().equals(zf.getStatusDesc())) {
							mp.setZfdoneZjh(1);
							toSave(mp);
							MsgPushServer.pushTextToUser(stockBasicService.getCodeName2(mp.getCode()) + " 增发已通过证监会核准！",
									"备注:" + mp.getRemark(), u);
						}
					}
				}
			}
		}
	}

	public void toSave(MonitorPoolTemp mp) {
		mp.setUpdatedate(DateUtil.getTodayIntYYYYMMDD());
		monitorPoolDao.save(mp);
	}

	// 公告监听
	public HashSet<String> listenerGg(int tradeDate) {
		HashSet<String> allf = new HashSet<String>();
		List<UserInfo> ulist = userService.getUserListForMonitorS1();
		for (UserInfo u : ulist) {
			List<MonitorPoolTemp> list = getList(u.getId(), "", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 0, 0, 0,
					0, 1);
			if (list != null) {
				// 公告提醒
				StringBuffer annc = new StringBuffer();
				for (MonitorPoolTemp mp : list) {
					if (ThsAnnSpider.getLastAnn(mp.getCode()) > tradeDate) {
						annc.append(stockBasicService.getCodeName2(mp.getCode())).append(",");
					}
				}
				if (annc.length() > 0) {
					MsgPushServer.pushTextToUser("最新公告", annc.toString(), u);
				}
			}
		}
		return allf;
	}

	// 用户服务到期
	public void userExpired() {
//		int today = DateUtil.getTodayIntYYYYMMDD();
//		int end = DateUtil.addDate(today, 1);
//		List<UserInfo> ulist = userService.getListForServiceEnd(1, today, end, EsQueryPageUtil.queryPage9999);
//		if (ulist != null) {
//			for (UserInfo u : ulist) {
//				if (u.getS1() == end) {// 提前一天提醒
//					MsgPushServer.pushSystemT1("服务续约提醒", "您的<系统登录>服务于" + u.getS1() + "到期,如有需要,请联系管理员及时续约！", u);
//				}
//			}
//		}
//
//		List<UserInfo> ulist2 = userService.getListForServiceEnd(2, today, end, EsQueryPageUtil.queryPage9999);
//		if (ulist2 != null) {
//			for (UserInfo u : ulist2) {
//				if (u.getS2() == end) {// 提前一天提醒
//					MsgPushServer.pushSystemT1("服务续约提醒", "您的<推荐>服务于" + u.getS2() + "到期,如有需要,请联系管理员及时续约！", u);
//				}
//			}
//		}
	}

	// 股东人数预警
	public HashSet<String> getListForFetchHolder() {
		HashSet<String> allf = new HashSet<String>();
		List<UserInfo> ulist = userService.getUserListForMonitorS1();
		for (UserInfo u : ulist) {
			List<MonitorPoolTemp> list = getList(u.getId(), "", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 1, 0, 0);
			if (list != null) {
				for (MonitorPoolTemp mp : list) {
					allf.add(mp.getCode());
				}
			}
		}
		return allf;
	}

	public void jobHolderWarning() {
		log.info("股东人数预警");
		List<UserInfo> ulist = userService.getUserListForMonitorS1();
		for (UserInfo u : ulist) {
			List<MonitorPoolTemp> list = getList(u.getId(), "", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 1, 0, 0);
			if (list != null) {
				StringBuffer sb = new StringBuffer();
				// 预警
				for (MonitorPoolTemp mp : list) {
					List<HolderNum> hml = chipsService.getHolderNumList45(mp.getCode());
					if (hml != null && hml.size() > 1) {
						HolderNum hn0 = hml.get(0);
						if (hn0.getDate() >= mp.getHolderNum()) {
							boolean islow = hml.get(1).getNum() > hn0.getNum();
							sb.append(stockBasicService.getCodeName2(mp.getCode()) + (islow ? ":下降" : ":上涨"))
									.append(Constant.DOU_HAO).append(Constant.HTML_LINE);
							mp.setHolderNum(DateUtil.getTodayIntYYYYMMDD());
							toSave(mp);
						}
					}
				}
				if (sb.length() > 0) {
					MsgPushServer.pushHtmlToUser("股东人数", sb.toString(), u);
				}
			}
		}
	}

	// 买点:地量
	public void jobBuyLowVolWarning() {
		// 暂时取消
//		List<UserInfo> ulist = userService.getUserListForMonitorS1();
//		for (UserInfo u : ulist) {
//			List<MonitorPoolTemp> list = getList(u.getId(), "", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 0, 1, 0);
//			if (list != null) {
//				StringBuffer sb = new StringBuffer();
//				Integer today = DateUtil.getTodayIntYYYYMMDD();
//				for (MonitorPoolTemp mp : list) {
//					mp.setBuyLowVol(60);// 理论是上面的参数，临时默认60,TODO 1年后去掉本行
//					EsQueryPageReq req = new EsQueryPageReq(mp.getBuyLowVol());
//					List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(mp.getCode(),
//							0, today, req, SortOrder.DESC);
//					TradeHistInfoDaliyNofq tday = l2.get(0);
//					double l = l2.stream().min(java.util.Comparator.comparingDouble(TradeHistInfoDaliyNofq::getVolume)).get()
//							.getVolume();
//					double factor = CurrencyUitl.topPriceN(l, 1.03);
//					if (tday.getVolume() <= factor) {
//						sb.append(stockBasicService.getCodeName2(mp.getCode())).append("->").append(mp.getBuyLowVol())
//								.append("天").append(Constant.HTML_LINE);
//					}
//				}
//				if (sb.length() > 0) {
//					MsgPushServer.pushSystemHtmlT2("流动性地量", sb.toString(), u);
//				}
//			}
//		}
	}

	// 大宗交易
	public void jobDzjyWarning() {
		List<UserInfo> ulist = userService.getUserListForMonitorS1();
		for (UserInfo u : ulist) {
			ThreadsUtil.sleepRandomSecBetween15And30();
			List<MonitorPoolTemp> list = getList(u.getId(), "", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 0, 0, 0,
					1, 0);
			List<String> l = new LinkedList<String>();
			if (list != null) {
				Integer today = DateUtil.getTodayIntYYYYMMDD();
				for (MonitorPoolTemp mp : list) {
					Dzjy dzjy = chipsService.getLastDzjy(mp.getCode());
					if (dzjy.getDate() > mp.getDzjy()) {
						l.add(mp.getCode());
						mp.setDzjy(today);
						toSave(mp);
					}
				}
				if (l.size() > 0) {
					StringBuffer sb = new StringBuffer();
					for (String s : l) {
						sb.append(stockBasicService.getCodeName2(s)).append(Constant.DOU_HAO);
					}
					MsgPushServer.pushTextToUser("关注票的大宗交易", sb.toString(), u);
				}
			}
		}
	}

	// 离线价格监听
	public void priceChk(List<TradeHistInfoDaliyNofq> listNofq, int tradeDate) {
		if (listNofq != null && listNofq.size() > 0) {
			Map<String, TradeHistInfoDaliyNofq> map = this.getPoolMap2(listNofq);
			List<UserInfo> ulist = userService.getUserListForMonitorS1();
			for (UserInfo u : ulist) {
				List<MonitorPoolTemp> list = this.getPoolListForMonitor(u.getId(), 0, 1, false);
				if (list != null) {
					List<String> ZengFaAuto = new LinkedList<String>();
					List<String> Other = new LinkedList<String>();
					for (MonitorPoolTemp cp : list) {
						String code = cp.getCode();
						if (cp.getDownPrice() <= 0 && cp.getDownTodayChange() <= 0 && cp.getUpPrice() <= 0
								&& cp.getUpTodayChange() <= 0) {
							log.info("{} 没有离线价格监听", code);
							continue;
						}
						TradeHistInfoDaliyNofq d = map.get(code);
						if (d != null) {
							if (MonitoringUitl.isOk(cp, d.getTodayChangeRate(), d.getHigh(), d.getLow())) {
								String s = "<a target='_blank' href='" + myUrlService.getUrl2_manual(code) + "'>"
										+ stockBasicService.getCodeName2(code) + "</a> "
										+ MonitorType.getCodeName(cp.getMonitor()) + " " //
										+ cp.getMsg() //
										+ " 今日涨幅:" + d.getTodayChangeRate() + "% 收盘价:" + d.getClosed()//
										+ " " + cp.getRemark();
								if (u.getId() == Constant.MY_ID) {// 管理员
									if (cp.getMonitor() == MonitorType.ZengFaAuto.getCode()) {
										ZengFaAuto.add(s);
									} else {
										Other.add(s);
									}
								} else {// 普通用户
									Other.add(s);
								}
							}
						}
					}
					// 价格
					StringBuffer s = new StringBuffer();
					for (String a : Other) {
						s.append(a).append(Constant.HTML_LINE);
					}
					for (String a : ZengFaAuto) {
						s.append(a).append(Constant.HTML_LINE);
					}
					if (s.length() > 0) {
						MsgPushServer.pushHtmlToUser("离线价格监听", s.toString(), u);
					}
				}
			}
		}
	}

	// 经营现金流转正监听
//	public void jobXjlWarning() {
//		List<UserInfo> ulist = userService.getUserListForMonitorS1();
//		for (UserInfo u : ulist) {
//			List<MonitorPoolTemp> list = this.getList(u.getId(), "", 0, 0, 0, 0, EsQueryPageUtil.queryPage9999, "", 0,
//					0, 1);
//			if (list != null) {
//				for (MonitorPoolTemp mp : list) {
//					FinanceBaseInfo fbi = financeService.getLastFinaceReport(mp.getCode());
//					if (fbi.getJyxjlce() > 0 || fbi.getMgjyxjl() > 0) {
//						WxPushUtil.pushSystem1(u.getWxpush(),
//								mp.getCode() + " 经营现金流净额已转正(" + fbi.getYear() + "年" + fbi.getQuarter() + "季度)");
//					}
//				}
//			}
//		}
//	}

	// 快预报监听
	public void kybMonitor() {
		List<UserInfo> ulist = userService.getUserListForMonitorS1();
		for (UserInfo u : ulist) {
			List<MonitorPoolTemp> list = this.getList(u.getId(), "", 0, 0, 1, 0, EsQueryPageUtil.queryPage9999, "", 0,
					0, 0);
			if (list != null) {
				StringBuffer sssb = new StringBuffer();
				for (MonitorPoolTemp mp : list) {
					if (mp.getYkb() > 0) {
						try {
							String code = mp.getCode();
							FinanceBaseInfo fbi = financeService.getLastFinaceReport(code);
							FinYjkb yjkb = financeService.getLastFinaceKbByReportDate(code, fbi.getYear(),
									fbi.getQuarter());
							boolean find = false;
							StringBuffer sb = new StringBuffer();

							// 业绩快报(准确的)
							if (yjkb != null && yjkb.getAnnDate() > mp.getYkb()) {
								sb.append(stockBasicService.getCodeName2(code));
								sb.append(",业绩快报");
								if (yjkb.getJlr() > 0) {
									sb.append(",盈利");
									find = true;
								} else if (yjkb.getJlr() < 0) {
									sb.append(",<font color='red'>[亏损]</font>");
									find = true;
								}
								if (find) {
									mp.setYkb(yjkb.getAnnDate());
									sb.append(",业绩同比:").append(CurrencyUitl.roundHalfUp(yjkb.getJlrtbzz())).append("%");
									sb.append(",营收同比:").append(CurrencyUitl.roundHalfUp(yjkb.getYyzsrtbzz()))
											.append("%");
								}
							}
							// 业绩预告(类似天气预报,可能不准)
							if (!find) {
								FinYjyg yjyg = financeService.getLastFinaceYgByReportDate(code, fbi.getYear(),
										fbi.getQuarter());
								if (yjyg != null && yjyg.getAnnDate() > mp.getYkb()) {
									sb.append(stockBasicService.getCodeName2(code));
									sb.append(",业绩预告");
									if (yjyg.getJlr() > 0) {
										sb.append(",盈利");
										find = true;
									} else if (yjyg.getJlr() < 0) {
										sb.append(",<font color='red'>[亏损]</font>");
										find = true;
									}
									if (find) {
										mp.setYkb(yjyg.getAnnDate());
										sb.append(",业绩同比:").append(CurrencyUitl.roundHalfUp(yjyg.getJlrtbzz()))
												.append("%");
									}
								}
							}
							if (!find) {
								if (fbi.getAnnDate() > mp.getYkb()) {
									sb.append(stockBasicService.getCodeName2(code));
									sb.append(",业绩报告");
									if (fbi.getGsjlr() > 0) {
										sb.append(",盈利");
										find = true;
									} else if (fbi.getGsjlr() < 0) {
										sb.append(",<font color='red'>[亏损]</font>");
										find = true;
									}
									if (find) {
										mp.setYkb(fbi.getAnnDate());
										sb.append(",业绩同比:").append(CurrencyUitl.roundHalfUp(fbi.getGsjlrtbzz()))
												.append("%,营收同比:").append(CurrencyUitl.roundHalfUp(fbi.getYyzsrtbzz()))
												.append("%");
									}
								}
							}
							if (find) {
								toSave(mp);
								sssb.append(sb.toString()).append(Constant.HTML_LINE);
							}
						} catch (Exception e) {
							ErrorLogFileUitl.writeError(e, "快预报预警", "", "");
						}
					}
				}

				if (sssb.length() > 0) {
					MsgPushServer.pushHtmlToUser("业绩快预报预警", sssb.toString(), u);
				}
			}
		}
	}

	// 删除退市数据：监听，模型
	public void deleteTsMoni() {
		Iterator<MonitorPoolTemp> it2 = monitorPoolDao.findAll().iterator();
		List<MonitorPoolTemp> removelist2 = new LinkedList<MonitorPoolTemp>();
		while (it2.hasNext()) {
			MonitorPoolTemp e = it2.next();
			if (!Constant.CODE_ON_STATUS.equals(this.stockBasicService.getCode(e.getCode()).getList_status())) {
				removelist2.add(e);// 已退市
			}
		}
		if (removelist2.size() > 0) {
			monitorPoolDao.deleteAll(removelist2);
		}

		Iterator<CodeBaseModel2> it = codeBaseModel2Dao.findAll().iterator();
		List<CodeBaseModel2> removelist1 = new LinkedList<CodeBaseModel2>();
		while (it.hasNext()) {
			CodeBaseModel2 e = it.next();
			if (!Constant.CODE_ON_STATUS.equals(this.stockBasicService.getCode(e.getCode()).getList_status())) {
				removelist1.add(e);// 已退市
			}
		}
		if (removelist1.size() > 0) {
			codeBaseModel2Dao.deleteAll(removelist1);
		}
	}

	public List<MonitorPoolTemp> getMonitorPool(String code) {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		queryBuilder = queryBuilder.withPageable(PageRequest.of(querypage.getPageNum(), querypage.getPageSize()));
		SearchQuery sq = queryBuilder.build();

		Page<MonitorPoolTemp> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
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
