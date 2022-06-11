package com.stable.service.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.MonitorPoolUserDao;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
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
	private Sort6Service sort6Service;
	@Autowired
	private Sort1ModeService sort1ModeService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private QibaoService qibaoService;

	public synchronized void runKLineModel(int date) {
//		if (!tradeCalService.isOpen(date)) {
//			date = tradeCalService.getPretradeDate(date);
//		}
		tradeDate = date;
		pre1Year = DateUtil.getPreYear(tradeDate);
		pre4Year = DateUtil.getPreYear(tradeDate, 4);
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();

		Map<String, MonitorPoolTemp> poolMap = monitorPoolService.getMonitorPoolMap();
		List<MonitorPoolTemp> poolList = new LinkedList<MonitorPoolTemp>();// TODO

		for (StockBaseInfo s : codelist) {
			try {
				this.processingByCode(s, poolMap, poolList, listLast, histMap);
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
	}

	private int tradeDate = 0;
	private int pre1Year = 0;// 一年以前
	private int pre4Year = 0;// 四年以前

	private void processingByCode(StockBaseInfo s, Map<String, MonitorPoolTemp> poolMap, List<MonitorPoolTemp> poolList,
			List<CodeBaseModel2> listLast, Map<String, CodeBaseModel2> histMap) {
		String code = s.getCode();
		// 监听池
		MonitorPoolTemp pool = poolMap.get(code);
		if (pool == null) {
			pool = new MonitorPoolTemp();
			pool.setCode(code);
		}
		poolList.add(pool);
		boolean onlineYear = stockBasicService.onlinePreYearChk(code, pre1Year);
		if (!onlineYear) {// 不买卖新股
			CodeBaseModel2 tone = new CodeBaseModel2();
			tone.setId(code);
			tone.setCode(code);
			tone.setDate(tradeDate);
			listLast.add(tone);
			return;
		}
		boolean online4Year = stockBasicService.onlinePreYearChk(code, pre4Year);
		CodeBaseModel2 newOne = histMap.get(s.getCode());
		if (newOne == null) {
			newOne = new CodeBaseModel2();
			newOne.setId(code);
			newOne.setCode(code);
		}
		// 最新收盘情况
		DaliyBasicInfo2 lastTrade = daliyBasicHistroyService.queryLastest(code, 0, 0);
		if (lastTrade == null) {
			lastTrade = new DaliyBasicInfo2();
		}
		// N年未大涨
		noup(online4Year, newOne, s.getList_date());
		// ==============技术面-量价==============
		// 一年新高
		year1(newOne, lastTrade);
		// 短线：妖股形态，短线拉的急，说明货多。一倍了，说明资金已经投入。新高:说明出货失败或者有更多的想法，要继续拉。
		sort1ModeService.sort1ModeChk(newOne, pool, tradeDate);
		// 均线排列，一阳穿N线
		LineAvgPrice.avgLineUp(s, newOne, avgService, code, tradeDate);
		// 基本面-疑似白马//TODO白马更多细节，比如市值，基金
		susWhiteHorses(code, newOne);
		// 短线模型
		sortModel(newOne, tradeDate);
		// 攻击形态
		sort0Service.attackAndW(newOne, tradeDate);
		// 起爆点
		qibaoService.qibao(tradeDate, newOne, pool);
	}

	private void year1(CodeBaseModel2 newOne, DaliyBasicInfo2 lastTrade) {
		TradeHistInfoDaliy high = daliyTradeHistroyService.queryYear1HighRecord(newOne.getCode(), tradeDate);
		redisUtil.set(RedisConstant.YEAR_PRICE_ + newOne.getCode(), high.getHigh());
		if (lastTrade.getClosed() > 0 && high.getHigh() > lastTrade.getClosed()
				&& CurrencyUitl.cutProfit(lastTrade.getClosed(), high.getHigh()) <= 15) {// 15%以内冲新高
			newOne.setShooting10(1);
		} else {
			newOne.setShooting10(0);
		}
	}

	private void susWhiteHorses(String code, CodeBaseModel2 newOne) {
		// 是否中线(60日线),市值300亿以上
		if (newOne.getMkv() > 200 && priceLifeService.getLastIndex(code) >= 80
				&& LineAvgPrice.isWhiteHorseForMidV2(avgService, code, newOne.getDate())) {
			newOne.setSusWhiteHors(1);
		} else {
			newOne.setSusWhiteHors(0);
		}
	}

	private void sortModel(CodeBaseModel2 newOne, int tradeDate) {
		String code = newOne.getCode();
		// 短线模型7(箱体震荡新高，是否有波浪走势)
		if (sort6Service.isWhiteHorseForSortV7(code, tradeDate)) {
			newOne.setSortMode7(1);
		} else {
			newOne.setSortMode7(0);
		}
	}

	// 周末计算-至少N年未大涨?
	private void noup(boolean online4Year, CodeBaseModel2 newOne, String listdatestr) {
		// 周末计算-至少N年未大涨?
		newOne.setZfjjup(0);
		newOne.setZfjjupStable(0);
		String code = newOne.getCode();
		if (online4Year) {
			int listdate = Integer.valueOf(listdatestr);
			newOne.setZfjjup(priceLifeService.noupYear(code, listdate));
			if (newOne.getZfjjup() >= 2) {
				newOne.setZfjjupStable(priceLifeService.noupYearstable(code, listdate));
			}
		}
	}

}
