package com.stable.service.trace;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.CodePoolService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.CodeModelService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodePool;

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
//
	private String OK = "基本面OK,疑是建仓";

//	private String NOT_OK = "系统默认NOT_OK";

	private double chkdouble = 80.0;// 10跌倒5.x

	public synchronized void start(int treadeDate, List<CodePool> list) {
		LineAvgPrice avg = new LineAvgPrice(avgService);
		log.info("code coop list:" + list.size());
		StringBuffer msg = new StringBuffer();
		StringBuffer mid = new StringBuffer();
		StringBuffer msg2 = new StringBuffer();
		if (list.size() > 0) {
			LinePrice lp = new LinePrice(daliyTradeHistroyService);
			for (CodePool m : list) {
				String code = m.getCode();
				boolean onlineYear = stockBasicService.online1YearChk(code, treadeDate);
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
					m.setMonitor(1);// 监听:0不监听，1大牛，2中线，3人工
				} else {
					if (m.getSuspectBigBoss() == 1) {
						msg2.append(code).append(",");
					}
					m.setSuspectBigBoss(0);
				}

				// 是否中线(60日线)
				if (priceLifeService.getLastIndex(code) >= 80 && avg.isWhiteHorseForMidV2(code, treadeDate)) {
					if (m.getInmid() == 0) {
						mid.append(code).append(",");
					}
					m.setInmid(1);
				} else {
					m.setInmid(0);
				}
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
		}
	}

	public synchronized void startManul() {
		int date = DateUtil.getTodayIntYYYYMMDD();
		date = tradeCalService.getPretradeDate(date);
		this.start(date, codeModelService.findBigBoss(date));
	}
}
