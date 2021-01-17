package com.stable.service.model;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.CodePoolService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodePool;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 基本面趋势票
 *
 */
@Service
@Log4j2
public class MiddleSortV1Service {
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
//
	private String OK = "基本面OK,疑是建仓";

//	private String NOT_OK = "系统默认NOT_OK";

	private double chkdouble = 80.0;// 10跌倒5.x

	public synchronized void start(int tradeDate, List<CodePool> list) {
		LineAvgPrice avg = new LineAvgPrice(avgService);
		log.info("code coop list:" + list.size());
		StringBuffer msg = new StringBuffer();
		StringBuffer mid = new StringBuffer();
		StringBuffer msg2 = new StringBuffer();
		StringBuffer msg3 = new StringBuffer();
		if (list.size() > 0) {
			LinePrice lp = new LinePrice(daliyTradeHistroyService);
			for (CodePool m : list) {
				String code = m.getCode();
				boolean onlineYear = stockBasicService.online1YearChk(code, tradeDate);
				if (!onlineYear) {
					log.info("{},Online 上市不足1年", code);
					continue;
				}
				boolean isBigBoss = false;
				if (m.isIsok()) {
					// 1年整幅未超过80%
					if (lp.priceCheckForMid(code, m.getUpdateDate(), chkdouble)) {
						isBigBoss = true;
					}
				}
				// 是否大牛
				if (isBigBoss) {
					if (m.getSuspectBigBoss() == 0) {
						msg.append(code).append(",");
					}
					m.setRemark(OK);
					m.setSuspectBigBoss(1);
					if (m.getMonitor() == 0) {
						m.setMonitor(1);// 监听:0不监听，1大牛，2中线，3人工
					}
				} else {
					if (m.getSuspectBigBoss() == 1) {
						msg2.append(code).append(",");
					}
					m.setSuspectBigBoss(0);
				}

				// 是否中线(60日线)
				if (priceLifeService.getLastIndex(code) >= 80 && avg.isWhiteHorseForMidV2(code, tradeDate)) {
					if (m.getInmid() == 0) {
						mid.append(code).append(",");
					}
					m.setInmid(1);
				} else {
					m.setInmid(0);
				}
				// 1大牛，2中线，3人工，4短线 // 箱体新高（3个月新高，短期有8%的涨幅）
				chk(m, code, tradeDate, msg);
			}
			codePoolService.saveAll(list);
			if (msg.length() > 0 || mid.length() > 0) {
				if (msg.length() > 0) {
					msg.insert(0, "新发现疑似主力建仓票:");
				}
				if (mid.length() > 0) {
					mid.insert(0, "新发现中线票:");
				}
				msg.append(mid.toString());
				WxPushUtil.pushSystem1(msg.toString());
			}
			if (msg2.length() > 0) {
				WxPushUtil.pushSystem1("踢出主力建仓票:" + msg2.toString());
			}
			if (msg3.length() > 0) {
				WxPushUtil.pushSystem1("股票池监听启动股票:" + msg3.toString());
			}
		}
	}

	public void sortv5(int tradeDate) {
		StringBuffer msg = new StringBuffer();
		List<Integer> pa = new ArrayList<Integer>();// 1大牛，2中线，3人工，4短线
		pa.add(1);
		pa.add(3);
		List<CodePool> list = codePoolService.queryForSortV5(pa);
		if (list != null && list.size() > 0) {
			for (CodePool cp : list) {
				String code = cp.getCode();
				log.info(cp);
				chk(cp, code, tradeDate, msg);
			}
		}
		if (msg.length() > 0) {
			WxPushUtil.pushSystem1("股票池监听启动股票:" + msg.toString());
		}
	}

	private void chk(CodePool m, String code, int treadeDate, StringBuffer msg3) {
		if ((m.getMonitor() == 1 || m.getMonitor() == 3) && isTodayPriceOk(code, treadeDate)
				&& isWhiteHorseForSortV5(code, treadeDate)) {
			msg3.append(code).append(",");
		}
	}

	/**
	 * 1.3个月新高，短期有9.5%的涨幅
	 */
	private boolean isTodayPriceOk(String code, int date) {
		EsQueryPageReq page = EsQueryPageUtil.queryPage60;
		// 3个月新高，22*3=60
		DaliyBasicInfo daliy = daliyBasicHistroyService.getDaliyBasicInfoByDate(code, date);
		List<TradeHistInfoDaliy> listD60 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
				daliy.getTrade_date(), page, SortOrder.DESC);
		if (listD60 == null || listD60.size() < page.getPageSize()) {
			log.info("{} 未获取到3个月的前复权交易记录", code);
			return false;
		}
		boolean isTopOK = true;
		for (TradeHistInfoDaliy td : listD60) {
			if (td.getHigh() > daliy.getHigh()) {
				isTopOK = false;
			}
		}
		if (isTopOK) {
			List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0,
					daliy.getTrade_date(), EsQueryPageUtil.queryPage5, SortOrder.DESC);
			for (TradeHistInfoDaliyNofq r : l2) {
				if (r.getTodayChangeRate() >= 9.5) {
					return true;
				}
			}
			log.info("{} 最近5个工作日无大涨交易", code);
		} else {
			log.info("{} 非3个月新高", code);
		}
		return false;
	}

	/**
	 * 2.均线
	 */
	private boolean isWhiteHorseForSortV5(String code, int date) {
		EsQueryPageReq req = EsQueryPageUtil.queryPage30;
		// 最近30条-倒序
		List<StockAvgBase> clist30 = avgService.queryListByCodeForModelWithLast60(code, date, req, true);
		StockAvgBase sa = clist30.get(0);
		if (sa.getAvgPriceIndex30() >= sa.getAvgPriceIndex60() && sa.getAvgPriceIndex20() >= sa.getAvgPriceIndex30()
				&& sa.getAvgPriceIndex20() >= sa.getAvgPriceIndex5()) {
			return true;
		}
		log.info("{} 均线不满足", code);
		return false;
	}

	public synchronized void startManul() {
		int date = DateUtil.getTodayIntYYYYMMDD();
		date = tradeCalService.getPretradeDate(date);
		this.start(date, codeModelService.findBigBoss(date));
	}
}
