package com.stable.service.datamv;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.BonusHistDao;
import com.stable.es.dao.base.DzjyDao;
import com.stable.es.dao.base.DzjyYiTimeDao;
import com.stable.es.dao.base.EsBuyBackInfoDao;
import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.es.dao.base.EsFinYjkbDao;
import com.stable.es.dao.base.EsFinYjygDao;
import com.stable.es.dao.base.EsHolderNumDao;
import com.stable.es.dao.base.EsHolderPercentDao;
import com.stable.es.dao.base.EsReducingHoldingSharesDao;
import com.stable.es.dao.base.EsStockBaseInfoDao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyDao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyNofqDao;
import com.stable.es.dao.base.FenHongDao;
import com.stable.es.dao.base.ForeignCapitalSumDao;
import com.stable.es.dao.base.JiejinDao;
import com.stable.es.dao.base.MonitorPoolUserDao;
import com.stable.es.dao.base.RzrqDaliyDao;
import com.stable.es.dao.base.RztjDao;
import com.stable.es.dao.base.UserDao;
import com.stable.es.dao.base.ZengFaDao;
import com.stable.es.dao.base.ZengFaDetailDao;
import com.stable.es.dao.base.ZengFaExtDao;
import com.stable.es.dao.base.ZengFaSummaryDao;
import com.stable.es.dao.base.ZhiYaDao;
import com.stable.service.TradeCalService;
import com.stable.utils.HttpUtil;
import com.stable.utils.SpringUtil;
import com.stable.vo.bus.BonusHist;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.FinYjkb;
import com.stable.vo.bus.FinYjyg;
import com.stable.vo.bus.ForeignCapitalSum;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.HolderPercent;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.ReducingHoldingShares;
import com.stable.vo.bus.RzrqDaliy;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaExt;
import com.stable.vo.bus.ZengFaSummary;
import com.stable.vo.bus.ZhiYa;

import lombok.extern.log4j.Log4j2;

@Service
@SuppressWarnings("rawtypes")
@Log4j2
public class DataMovingNewer implements InitializingBean {

	@Autowired
	private TradeCalService tradeCalService;

	public Map<String, ElasticsearchRepository> daoMap = new HashMap<String, ElasticsearchRepository>();
	public Map<String, Class> clzMap = new HashMap<String, Class>();

	private void init() {
		daoMap.put("UserInfo", SpringUtil.getBean(UserDao.class));
		clzMap.put("UserInfo", UserInfo.class);
		daoMap.put("Concept", SpringUtil.getBean(EsConceptDao.class));
		clzMap.put("Concept", Concept.class);
		daoMap.put("ZhiYa", SpringUtil.getBean(ZhiYaDao.class));
		clzMap.put("ZhiYa", ZhiYa.class);
		daoMap.put("ZengFaSummary", SpringUtil.getBean(ZengFaSummaryDao.class));
		clzMap.put("ZengFaSummary", ZengFaSummary.class);
		daoMap.put("ZengFaExt", SpringUtil.getBean(ZengFaExtDao.class));
		clzMap.put("ZengFaExt", ZengFaExt.class);
		daoMap.put("ZengFaDetail", SpringUtil.getBean(ZengFaDetailDao.class));
		clzMap.put("ZengFaDetail", ZengFaDetail.class);
		daoMap.put("ZengFa", SpringUtil.getBean(ZengFaDao.class));
		clzMap.put("ZengFa", ZengFa.class);
		daoMap.put("Rztj", SpringUtil.getBean(RztjDao.class));
		clzMap.put("Rztj", ZhiYa.class);
		daoMap.put("RzrqDaliy", SpringUtil.getBean(RzrqDaliyDao.class));
		clzMap.put("RzrqDaliy", RzrqDaliy.class);
		daoMap.put("MonitorPoolTemp", SpringUtil.getBean(MonitorPoolUserDao.class));
		clzMap.put("MonitorPoolTemp", MonitorPoolTemp.class);
		daoMap.put("Jiejin", SpringUtil.getBean(JiejinDao.class));
		clzMap.put("Jiejin", Jiejin.class);
		daoMap.put("FenHong", SpringUtil.getBean(FenHongDao.class));
		clzMap.put("FenHong", FenHong.class);
		daoMap.put("StockBaseInfo", SpringUtil.getBean(EsStockBaseInfoDao.class));
		clzMap.put("StockBaseInfo", StockBaseInfo.class);
		daoMap.put("ReducingHoldingShares", SpringUtil.getBean(EsReducingHoldingSharesDao.class));
		clzMap.put("ReducingHoldingShares", ReducingHoldingShares.class);
		daoMap.put("HolderPercent", SpringUtil.getBean(EsHolderPercentDao.class));
		clzMap.put("HolderPercent", HolderPercent.class);
		daoMap.put("HolderNum", SpringUtil.getBean(EsHolderNumDao.class));
		clzMap.put("HolderNum", HolderNum.class);
		// daoMap.put("FinanceBaseInfo",sp.getBean(EsFinanceBaseInfoDao.class));
		// clzMap.put("FinanceBaseInfo", FinanceBaseInfo.class);
		daoMap.put("CodeConcept", SpringUtil.getBean(EsCodeConceptDao.class));
		clzMap.put("CodeConcept", CodeConcept.class);
		// daoMap.put("CodeBaseModel2", SpringUtil.getBean(EsCodeBaseModel2Dao.class));
		// clzMap.put("CodeBaseModel2", CodeBaseModel2.class);
		daoMap.put("BuyBackInfo", SpringUtil.getBean(EsBuyBackInfoDao.class));
		clzMap.put("BuyBackInfo", BuyBackInfo.class);
		daoMap.put("DzjyYiTime", SpringUtil.getBean(DzjyYiTimeDao.class));
		clzMap.put("DzjyYiTime", DzjyYiTime.class);
		daoMap.put("Dzjy", SpringUtil.getBean(DzjyDao.class));
		clzMap.put("Dzjy", Dzjy.class);
		daoMap.put("BonusHist", SpringUtil.getBean(BonusHistDao.class));
		clzMap.put("BonusHist", BonusHist.class);
		daoMap.put("DaliyBasicInfo2", SpringUtil.getBean(EsDaliyBasicInfoDao.class));
		clzMap.put("DaliyBasicInfo2", DaliyBasicInfo2.class);
		
		daoMap.put("TradeHistInfoDaliy", SpringUtil.getBean(EsTradeHistInfoDaliyDao.class));
		clzMap.put("TradeHistInfoDaliy", TradeHistInfoDaliy.class);
		daoMap.put("TradeHistInfoDaliyNofq", SpringUtil.getBean(EsTradeHistInfoDaliyNofqDao.class));
		clzMap.put("TradeHistInfoDaliyNofq", TradeHistInfoDaliyNofq.class);
		daoMap.put("FinYjkb", SpringUtil.getBean(EsFinYjkbDao.class));
		clzMap.put("FinYjkb", FinYjkb.class);
		daoMap.put("FinYjyg", SpringUtil.getBean(EsFinYjygDao.class));
		clzMap.put("FinYjyg", FinYjyg.class);
		daoMap.put("ForeignCapitalSum", SpringUtil.getBean(ForeignCapitalSumDao.class));
		clzMap.put("ForeignCapitalSum", ForeignCapitalSum.class);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		init();
	}

	@Autowired
	private DataMovingProvider dataMovingProvider;

	public Dw fetchData(String tableName, int pageNum, int pageSize) {
		log.info("tableName=" + tableName + ",pageSize=" + pageSize + ",pageNum=" + pageNum);
		return dataMovingProvider.getData(tableName, pageNum, pageSize);
	}

	@javax.annotation.PostConstruct
	public void testAll() throws Exception {
		log.info("每月-开始同步日历");
		tradeCalService.josSynTradeCal();
		new Thread(new Runnable() {
			@Override
			public void run() {
				init();
				int pageSize = 100;
				for (String tableName : clzMap.keySet()) {
					executeMoveByTable(tableName, pageSize);
					System.err.println("=end :>" + tableName);
					try {
						Thread.sleep(10 * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				System.err.println("=end=ALL");
			}
		}).start();

//		System.exit(0);
	}

	public void testOne() throws Exception {
		init();
		String tableName = "UserInfo";
		int pageSize = 100;
		executeMoveByTable(tableName, pageSize);
		System.err.println("=end=");
//		System.exit(0);
	}

	private String fetchDataByHttp(String tableName, int pageNum, int pageSize) {
		String url = "http://106.52.95.147:9999/web/datamv/get?tableName=" + tableName + "&pageNum=" + pageNum
				+ "&pageSize=" + pageSize;
		log.info(url);
		return HttpUtil.doGet2(url);
	}

	@SuppressWarnings("unchecked")
	public void executeMoveByTable(String tableName, int pageSize) {
		List list = null;
		int pageNum = 1;
		long tot = 0;
		while (true) {
			String json = fetchDataByHttp(tableName, pageNum, pageSize);
			// System.err.println(json);
			Dw<?> res = JSON.toJavaObject(JSON.parseObject(json), Dw.class);
			if (res == null) {
				break;
			}
			if (tot == 0 && res.getTableSize() > 0) {
				tot = res.getTableSize();
			}

			list = new LinkedList();
			for (Object obj : res.getTableData()) {
				JSONObject jo = (JSONObject) obj;
				list.add(jo.toJavaObject(clzMap.get(tableName)));
			}
			if (list.size() > 0) {
				daoMap.get(tableName).saveAll(list);
			}
			tot = tot - res.batchSize;
			if (tot <= 0) {
				break;
			}
			pageNum++;
		}
	}
}
