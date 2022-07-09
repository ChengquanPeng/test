
package com.stable.service.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.PlateService;
import com.stable.service.TradeCalService;
import com.stable.utils.ThreadsUtil;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RunModelService {

	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private CodeModelKLineService codeModelKLineService;
	@Autowired
	private PlateService plateService;

	public synchronized void runModel(int date, boolean isweekend) {
		log.info("CodeModel processing request date={}", date);
		if (!tradeCalService.isOpen(date)) {
			date = tradeCalService.getPretradeDate(date);
		}
		log.info("Actually processing request date={}", date);

		if (isweekend) {
			// 周末,先跑基本面在跑技术面
			codeModelService.runModel1(date, true);
			ThreadsUtil.sleepRandomSecBetween15And30();
			codeModelKLineService.runKLineModel1(date);
		} else {
			codeModelKLineService.runKLineModel1(date);
			ThreadsUtil.sleepRandomSecBetween15And30();
			codeModelService.runModel1(date, false);
		}

		ThreadsUtil.sleepRandomSecBetween15And30();
		plateService.getPlateStat();
	}
}
