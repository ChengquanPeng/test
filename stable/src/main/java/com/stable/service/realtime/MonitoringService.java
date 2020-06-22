package com.stable.service.realtime;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.enums.BuyModelType;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.service.TradeCalService;
import com.stable.service.model.UpModelLineService;
import com.stable.service.model.data.AvgService;
import com.stable.service.trace.BuyTraceService;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BuyTrace;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MonitoringService {
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private UpModelLineService upModelLineService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private TickDataService tickDataService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private BuyTraceService buyTraceService;

	private EsQueryPageReq querypage = new EsQueryPageReq(1000);

	private Map<String, RealtimeDetailsAnalyzer> map = null;

	public synchronized void startObservable() {
		String date = DateUtil.getTodayYYYYMMDD();
		if (!tradeCalService.isOpen(Integer.valueOf(date))) {
			WxPushUtil.pushSystem1("非交易日结束监听");
			return;
		}
		long now = new Date().getTime();
		long isAlivingMillis = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 15:00:00").getTime();
		if (now > isAlivingMillis) {// 已经超时
			return;
		}
		List<RealtimeDetailsAnalyzer> list = new LinkedList<RealtimeDetailsAnalyzer>();
		String observableDate = tradeCalService.getPretradeDate(date);
		try {

			List<ModelV1> olist = upModelLineService.getListByCode(null, observableDate, "1", null, null, querypage, 4);

			log.info("observableDate:" + observableDate);
			if (olist == null || olist.size() <= 0) {
				WxPushUtil.pushSystem1("交易日" + observableDate + "没有白马股，休眠线程！到15:00");
			} else {
				map = new ConcurrentHashMap<String, RealtimeDetailsAnalyzer>();
				olist.forEach(x -> {
					log.info(x);
				});
				olist.forEach(x -> {
					RealtimeDetailsAnalyzer task = new RealtimeDetailsAnalyzer(x, daliyBasicHistroyService,
							avgService.queryListByCodeForRealtime(x.getCode(), x.getDate()), tickDataService,
							stockBasicService, buyTraceService);
					new Thread(task).start();
					list.add(task);
					map.put(x.getCode(), task);
				});
				WxPushUtil.pushSystem1("交易日" + observableDate + "开始监听实时交易，监听数量:" + olist.size());
			}

			long from3 = new Date().getTime();
			int millis = (int) ((isAlivingMillis - from3));
			if (millis > 0) {
				Thread.sleep(millis);
			}

			// 到点停止所有线程
			for (RealtimeDetailsAnalyzer t : list) {
				t.stop();
			}
			WxPushUtil.pushSystem1("交易日结束监听");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			map = null;
		}
	}

	public BuyTrace buyAndStopThread(String code) {
		if (map != null) {
			if (map.containsKey(code)) {
				RealtimeDetailsAnalyzer analyzer = map.get(code);
				analyzer.stop();
				Double buyPrice = analyzer.getBuyPrice();
				if (buyPrice != null) {
					BuyTrace bt = new BuyTrace();
					bt.setBuyDate(Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
					bt.setBuyModelType(BuyModelType.B1.getCode());
					bt.setBuyPrice(buyPrice);
					bt.setCode(code);
					bt.setId();
					bt.setStatus(2);
					bt.setProgram(analyzer.isPg() ? 1 : 2);
					bt.setCurrMkt(analyzer.isCurrMkt() ? 1 : 2);
					buyTraceService.addToTrace(bt);
					log.info("已成交:{}" + bt);
					return bt;
				}
				throw new RuntimeException("buyPrice is null");
			}
			throw new RuntimeException("map not containsKey ");
		}
		throw new RuntimeException("map is null");
	}
}
