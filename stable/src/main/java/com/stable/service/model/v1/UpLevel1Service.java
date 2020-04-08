package com.stable.service.model.v1;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.AvgService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.StrongService;
import com.stable.service.TickDataService;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.PriceLife;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UpLevel1Service {

	@Autowired
	private StrongService strongService;
	@Autowired
	private TickDataService tickDataService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private AvgService avgService;

	private final EsQueryPageReq queryPage = new EsQueryPageReq(250);

	public void runModel(ModelV1 mv1) {
		if (!stockBasicService.online1Year(mv1.getCode())) {
			log.info("Online 不足1年，code={}", mv1.getCode());
			return;
		}

		DaliyBasicInfo lastDate = this.strongCheck(mv1);
		this.tickDataCheck(mv1);
		this.priceIndex(mv1);
		avgService.checkAvg(mv1, lastDate.getTrade_date());
	}

	// 收盘价介于最高价和最低价的index
	private void priceIndex(ModelV1 mv1) {
		PriceLife pl = priceLifeService.getPriceLife(mv1.getCode());
		if (mv1.getClose() <= pl.getLowest()) {
			mv1.setPriceIndex(0);
		} else if (mv1.getClose() >= pl.getHighest()) {
			mv1.setPriceIndex(100);
		} else {
			double base = pl.getHighest() - pl.getLowest();
			double diff = mv1.getClose() - pl.getLowest();
			int present = Double.valueOf(diff / base * 100).intValue();
			mv1.setPriceIndex(present);
		}
	}

	// 1强势:次数和差值:3/5/10/20/120/250天
	private DaliyBasicInfo strongCheck(ModelV1 mv1) {
		return strongService.checkStrong(mv1);
	}

	// 2交易方向:次数和差值:3/5/10/20/120/250天
	// 3程序单:次数:3/5/10/20/120/250天
	private void tickDataCheck(ModelV1 mv1) {
		String code = mv1.getCode();
		List<TickDataBuySellInfo> list = this.tickDataService.list(code, null, null, queryPage);

		// check-3
		int wayTimes3 = 0;
		int index = 3;
		long wayDef3 = 0;
		int pgmTimes3 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes3++;
			}
			wayDef3 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes3++;
			}
		}
		mv1.setWayTimes3(wayTimes3);
		mv1.setWayDef3(wayDef3);
		mv1.setPgmTimes3(pgmTimes3);
		// check-5
		int wayTimes5 = 0;
		index = 5;
		long wayDef5 = 0;
		int pgmTimes5 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes5++;
			}
			wayDef5 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes5++;
			}
		}
		mv1.setWayTimes5(wayTimes5);
		mv1.setWayDef5(wayDef5);
		mv1.setPgmTimes5(pgmTimes5);
		// check-10
		int wayTimes10 = 0;
		index = 10;
		long wayDef10 = 0;
		int pgmTimes10 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes10++;
			}
			wayDef10 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes10++;
			}
		}
		mv1.setWayTimes10(wayTimes10);
		mv1.setWayDef10(wayDef10);
		mv1.setPgmTimes10(pgmTimes10);

		// check-20
		int wayTimes20 = 0;
		index = 20;
		long wayDef20 = 0;
		int pgmTimes20 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes20++;
			}
			wayDef20 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes20++;
			}
		}
		mv1.setWayTimes20(wayTimes20);
		mv1.setWayDef20(wayDef20);
		mv1.setPgmTimes20(pgmTimes20);
		// check-120

		if (list.size() < 120) {
			return;
		}
		index = 120;
		int wayTimes120 = 0;
		long wayDef120 = 0;
		int pgmTimes120 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes120++;
			}
			wayDef120 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes120++;
			}
		}
		mv1.setWayTimes120(wayTimes120);
		mv1.setWayDef120(wayDef120);
		mv1.setPgmTimes120(pgmTimes120);
		// check-250
		int wayTimes250 = 0;
		index = list.size();
		long wayDef250 = 0;
		int pgmTimes250 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes250++;
			}
			wayDef250 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes250++;
			}
		}
		mv1.setWayTimes250(wayTimes250);
		mv1.setWayDef250(wayDef250);
		mv1.setPgmTimes250(pgmTimes250);
	}
}
