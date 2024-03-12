package com.stable.service.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.MonitorPoolUserDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.prd.QxService;
import com.stable.service.model.prd.SzxService;
import com.stable.service.model.prd.V1XipanService;
import com.stable.service.model.prd.V2NXipanService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.TagUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelKLineService {
	@Autowired
	private MonitorPoolUserDao monitorPoolDao;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private WebModelService modelWebService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private Sort0Service sort0Service;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private QxService qbQxService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private V1XipanService v1XipanService;
	@Autowired
	private V2NXipanService nxService;
	@Autowired
	private SzxService szxService;
	@Autowired
	private TradeCalService tradeCalService;

	private void initDate(int t) {
//		if (!tradeCalService.isOpen(date)) {
//		date = tradeCalService.getPretradeDate(date);
//	}
		tradeDate = t;
		codeModelService.tradeDate = t;
		pre1Year = DateUtil.getPreYear(tradeDate);
		pre3Year = DateUtil.getPreYear(tradeDate, 3);
		nextTadeDate = tradeCalService.getNextDate(tradeDate);
	}

	public synchronized void runKLineModel1(int date) {
		this.initDate(date);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();

		Map<String, MonitorPoolTemp> poolMap = monitorPoolService.getMonitorPoolMap();
		List<MonitorPoolTemp> poolList = new LinkedList<MonitorPoolTemp>();

		String code = null;
		for (StockBaseInfo s : codelist) {
			try {
				// 监听池
				code = s.getCode();
				this.processingByCode(s, codeModelService.getPool(code, poolMap, poolList), listLast, histMap);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
			}
		}
		if (listLast.size() > 0) {
			codeBaseModel2Dao.saveAll(listLast);
		}
		if (poolList.size() > 0) {
			monitorPoolDao.saveAll(poolList);
		}

		log.info("KLine基本完成");
	}

	public synchronized void runByCode(String code, int t) {
		this.initDate(t);
		// 监听池
		MonitorPoolTemp pool = codeModelService.getPool(code);
		StockBaseInfo s = stockBasicService.getCode(code);
		Map<String, CodeBaseModel2> histMap = new HashMap<String, CodeBaseModel2>();
		histMap.put(code, this.modelWebService.getLastOneByCode2(code));
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
		this.processingByCode(s, pool, listLast, histMap);
//		if (listLast.size() > 0) {
//			codeBaseModel2Dao.saveAll(listLast);
//			log.info(listLast.get(0).toString());
//		}
//		monitorPoolDao.save(pool);
		log.info("KLine基本完成 for code:" + code);
	}

	private int tradeDate = 0;
	private int pre1Year = 0;// 一年以前
	private int pre3Year = 0;// 三年以前

	private int nextTadeDate = 0;

	private void processingByCode(StockBaseInfo s, MonitorPoolTemp pool, List<CodeBaseModel2> listLast,
			Map<String, CodeBaseModel2> histMap) {
		String code = s.getCode();
		boolean onlineYear = stockBasicService.onlinePreYearChk(code, pre1Year);
		if (!onlineYear) {// 不买卖新股
			CodeBaseModel2 tone = new CodeBaseModel2();
			tone.setId(code);
			tone.setCode(code);
			tone.setDate(tradeDate);
			listLast.add(tone);
			return;
		}
		boolean online3Year = stockBasicService.onlinePreYearChk(code, pre3Year);
		CodeBaseModel2 newOne = histMap.get(s.getCode());
		if (newOne == null) {
			newOne = new CodeBaseModel2();
			newOne.setId(code);
			newOne.setCode(code);
		}
		listLast.add(newOne);

		// 排除3:排除退市股票&ST
		if (stockBasicService.is_ST_And_TuiShi(s.getName(), code)) {
			qbQxService.setQxRes(newOne, pool, true, true);
			qbQxService.setSzxRes(newOne, pool);
			szxService.setSzxRes(newOne, pool);
			v1XipanService.resetXiPan(newOne);
			nxService.resetNxiPan(newOne);
			pool.setXpPrice(0);
			newOne.setShooting11(0);
			newOne.setShootingw(0);
			log.info("排除退市股票&ST:" + code);
			return;
		}

		// N年未大涨
		noup(online3Year, newOne, s.getList_date());
		// ==============技术面-量价==============
		// 3个月新高
//		year1(newOne, lastTrade);
		newOne.setShooting10(0);

		// 短线：妖股形态，短线拉的急，说明货多。
		// 一倍：说明资金已经投入，赶鸭子上架。
		// 新高:说明出货失败或者有更多的想法，要继续拉。
		// 调整或小平台：3-5天，时间太久容易出货
		// 买在新高，做好止损止盈应对策略。
		// sort1ModeService.sort1ModeChk(newOne, pool, tradeDate);
		// 收集筹码的短线-拉过一波，所以市值可以大一点，-已废弃

//		newOne.setSortChips(0);
//		if (online4Year && isSamll && chipsSortService.isCollectChips(code, tradeDate)) {
//			newOne.setSortChips(1);
//			log.info("{} 主力筹码收集", code);
//		}
		// 交易面-均线-疑似白马
		// susWhiteHorses(code, newOne);
		// 短线模型(箱体震荡-已废弃，实际是半年新高)
		// sortModel(newOne, tradeDate);
		// 攻击形态
		sort0Service.attackAndW(newOne, tradeDate);
		this.cutMkv(newOne, s);
		boolean isSamll = codeModelService.isSmallStock(newOne.getMkv(), newOne.getActMkv());
		// 底部优质大票
		if (TagUtil.isDibuOKBig(isSamll, newOne)) {
			if (s.getName().contains("银行") || s.getName().contains("证券")) {
				newOne.setShooting11(0);
			} else {
				newOne.setShooting11(1);
			}
		} else {
			newOne.setShooting11(0);
		}
		// 起爆点
		try {
			qbQxService.qx(tradeDate, newOne, pool, isSamll, nextTadeDate);
			szxService.szx(tradeDate, newOne, pool, isSamll);
			v1XipanService.xipanQb(tradeDate, newOne, isSamll, nextTadeDate);
			nxService.nxipan(tradeDate, newOne, nextTadeDate);
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, s.getCode(), tradeDate + "", "起爆");
		}

		if (newOne.getPrice3m() > 0) {
			pool.setXpPrice(newOne.getPrice3m());
		} else {
			pool.setXpPrice(0);
		}
	}

	public void susWhiteHorses(String code, CodeBaseModel2 newOne) {
		// 是否中线(60日线),市值300亿以上
		if (newOne.getMkv() > 200 && priceLifeService.getLastIndex(code) >= 80
				&& LineAvgPrice.isWhiteHorseForMidV2(avgService, code, newOne.getDate())) {
			newOne.setSusWhiteHors(1);
		} else {
			newOne.setSusWhiteHors(0);
		}
	}

//	public void sortModel(CodeBaseModel2 newOne, int tradeDate) {
//		newOne.setSortMode7(0);// 箱体震荡实际就是半年新高，暂时移除
//		String code = newOne.getCode();
//		// 短线模型7(箱体震荡新高，是否有波浪走势)
//		if (sort6Service.isWhiteHorseForSortV7(code, tradeDate)) {
//			newOne.setSortMode7(1);
//		} else {
//			newOne.setSortMode7(0);
//		}
//	}

	// 周末计算-至少N年未大涨?
	private void noup(boolean online4Year, CodeBaseModel2 newOne, String listdatestr) {
		// 周末计算-至少N年未大涨?
		newOne.setZfjjup(0);
		newOne.setZfjjupStable(0);
		String code = newOne.getCode();
		if (online4Year) {
			int listdate = Integer.valueOf(listdatestr);
			newOne.setZfjjup(priceLifeService.noupYear(code, listdate));
			if (newOne.getZfjjup() >= 1) {
				newOne.setZfjjupStable(priceLifeService.noupYearstable(code, listdate));
			}
		}
	}

	private void cutMkv(CodeBaseModel2 newOne, StockBaseInfo s) {
		String code = newOne.getCode();
		// 最新收盘情况
		DaliyBasicInfo2 lastTrade = daliyBasicHistroyService.queryLastest(code, 0, 0);
		if (lastTrade == null) {
			lastTrade = new DaliyBasicInfo2();
		}
		double mkv = lastTrade.getCircMarketVal();// 流通市值
		newOne.setPb(lastTrade.getPb());// 市盈率ttm
		newOne.setPettm(lastTrade.getPeTtm());// 市盈率ttm

		if (mkv <= 0) {
			ErrorLogFileUitl.writeError(null, code + "," + s.getName() + ",无最新流通市值mkv", tradeDate + "", "");
			DaliyBasicInfo2 ltt = daliyBasicHistroyService.queryLastest(code, 0, 1);
			if (ltt != null) {
				mkv = ltt.getCircMarketVal();
			}
		}
		// 市值-死筹计算
		newOne.setMkv(mkv);
		if (mkv > 0 && s.getCircZb() > 0) {// 5%以下的流通股份
			newOne.setActMkv(CurrencyUitl.roundHalfUp(Double.valueOf(mkv * (100 - s.getCircZb()) / 100)));
		} else {
			newOne.setActMkv(mkv);
		}
	}

//	@javax.annotation.PostConstruct
	public void rerun() {
		System.err.println("======== start1 ========");
		new Thread(new Runnable() {
			public void run() {
				System.err.println("======== start2 ========");
//				int date = 20240311;
//				runKLineModel1(date);
//				runByCode("600979", date);
				System.err.println("======== end ========");
				ThreadsUtil.sleepRandomSecBetween5And15Ths();
				System.err.println("======== start3 ========");
				System.exit(0);
			}
		}).start();
	}

//	@javax.annotation.PostConstruct
	public void test() {
		new Thread(new Runnable() {
			public void run() {
				int tradeDate1 = 20230512;
				int pre1Year1 = DateUtil.getPreYear(tradeDate1);
				List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
				Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
				List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
				for (StockBaseInfo s : codelist) {
					try {
						log.info("KLine init:{}", s.getCode());
						if (stockBasicService.onlinePreYearChk(s.getCode(), pre1Year1)) {
							CodeBaseModel2 newOne = histMap.get(s.getCode());
							if (newOne == null) {
								newOne = new CodeBaseModel2();
								newOne.setId(s.getCode());
								newOne.setCode(s.getCode());
							}
							nxService.nxipan(tradeDate1, newOne, nextTadeDate);
							if (newOne.getNxipan() == 1) {
								listLast.add(newOne);
							}
						}
					} catch (Exception e) {
						ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
					}
				}
				if (listLast.size() > 0) {
					codeBaseModel2Dao.saveAll(listLast);
				}
				log.info("done init:{}", listLast.size());
			}
		}).start();
	}
}
