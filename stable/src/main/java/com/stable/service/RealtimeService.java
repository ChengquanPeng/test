package com.stable.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.model.UpModelLineService;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

@Service
public class RealtimeService {
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private UpModelLineService upModelLineService;
	private boolean isAliving = false;
	private EsQueryPageReq querypage = new EsQueryPageReq(1000);
	private long ONE_MIN = 1 * 60 * 1000;// 1MIN

	public synchronized void startObservable() {
		if (isAliving) {
			return;
		}
		String date = DateUtil.getTodayYYYYMMDD();
		if (!tradeCalService.isOpen(Integer.valueOf(date))) {
			return;
		}
		String observableDate = tradeCalService.getPretradeDate(date);
		isAliving = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//TODO
					List<ModelV1> olist = upModelLineService.getListByCode(null, observableDate, "1", null, null,
							querypage);
					long emillis = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 21:00:00").getTime();
					if (olist == null || olist.size() <= 0) {
						WxPushUtil.pushSystem1("交易日" + observableDate + "没有白马股，休眠线程！到15:00");
						long from3 = new Date().getTime();
						int millis = (int) ((emillis - from3));
						if (millis > 0) {
							Thread.sleep(millis);
						}
					} else {
						while (true) {
							olist.forEach(x -> {

							});
							Thread.sleep(ONE_MIN);// 1分钟
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					isAliving = false;
				}
			}
		}).start();
	}
}
