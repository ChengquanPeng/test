package com.stable.service.model.data;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stable.service.TickDataService;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class LineTickData {

	private TickDataService tickDataService;
	private ModelContext cxt;
	private List<DaliyBasicInfo> dailyList;

	public LineTickData(ModelContext cxt, List<DaliyBasicInfo> dailyList, TickDataService tickDataService) {
		this.cxt = cxt;
		this.dailyList = dailyList;
		this.tickDataService = tickDataService;
	}

	// 2交易方向:次数和差值:3/5/10/20/120/250天
	// 3程序单:次数:3/5/10/20/120/250天
	private final EsQueryPageReq queryPage = new EsQueryPageReq(5);

	public void tickDataInfo() {
		DaliyBasicInfo firstFiveDate = dailyList.get(4);
		List<TickDataBuySellInfo> list = tickDataService.listForModel(cxt.getCode(), firstFiveDate.getTrade_date(),
				cxt.getDate(), queryPage);
		if (list.size() < 5) {
			log.error("size < 5");
			cxt.setDropOutMsg("每日指标记录小于5条,checkStrong get size<5");
			cxt.addDetailDesc("每日指标记录小于5条,checkStrong get size<5");
			return;
		}
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
		cxt.setWayTimes3(wayTimes3);
		cxt.setWayDef3(wayDef3);
		cxt.setPgmTimes3(pgmTimes3);
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
		cxt.setWayTimes5(wayTimes5);
		cxt.setWayDef5(wayDef5);
		cxt.setPgmTimes5(pgmTimes5);

		int sortWay = 0;
		boolean s3 = (cxt.getWayDef3() > 0 && cxt.getWayTimes3() > 0);
		boolean s5 = (cxt.getWayDef5() > 0 && cxt.getWayTimes5() > 0);
		if (s3 || s5) {
			sortWay = 1;
		}
		if (s3 && s5) {
			sortWay = 2;
		}
		cxt.setSortWay(sortWay);

		int sortPgm = 0;
		boolean p3 = (cxt.getPgmTimes3() > 0);
		boolean p5 = (cxt.getPgmTimes5() > 0);

		if (p3 || p5) {
			sortPgm = 1;
		}
		if (p3 && p5) {
			sortPgm = 2;
		}
		cxt.setSortPgm(sortPgm);
	}

}
