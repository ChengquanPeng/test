package com.stable.service.trace;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.enums.StockAType;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.retrace.TraceSortv1Vo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class HistTraceService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private TickDataService tickDataService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

	public static final Semaphore semp = new Semaphore(1);

	EsQueryPageReq queryPage = new EsQueryPageReq(10);

	public void sortv1(String startDate, String endDate) {
		try {
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusList();
			if (StringUtils.isBlank(startDate)) {
				startDate = "20200101";
			}
			if (StringUtils.isBlank(endDate)) {
				endDate = DateUtil.getTodayYYYYMMDD();
			}
			log.info("startDate={},endDate={}", startDate, endDate);
			try {
				boolean getLock = semp.tryAcquire(1, TimeUnit.HOURS);
				if (!getLock) {
					log.warn("No Locked");
					return;
				}
				log.info("Get Locked");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			List<TraceSortv1Vo> samples = new LinkedList<TraceSortv1Vo>();
			for (StockBaseInfo s : codelist) {
				StockAType sa = StockAType.formatCode(s.getCode());
				if (StockAType.KCB == sa) {
					continue;
				}
				try {
					JSONArray array2 = tushareSpider.getStockDaliyTrade(s.getTs_code(), null, startDate, endDate);
					if (array2 != null && array2.size() > 0) {
						for (int ij = 0; ij < array2.size(); ij++) {
							DaliyBasicInfo d2 = new DaliyBasicInfo();
							d2.setCode(s.getCode());
							d2.daily(array2.getJSONArray(ij));
							TraceSortv1Vo tv = tickDataService.check(d2.getCode(), null, d2);
							if (tv != null) {
								samples.add(tv);
							}
						}
					}

				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, s.getCode(), startDate, startDate);
				}
				ThreadsUtil.thsSleepRandom();
			}

			log.info("获取样本数:" + samples.size());
			for (TraceSortv1Vo t1 : samples) {
				try {
					List<DaliyBasicInfo> dailyList = daliyBasicHistroyService.queryListByCode(
							t1.getDaliyBasicInfo().getCode(), t1.getDaliyBasicInfo().getTrade_date(), 0, queryPage,
							SortOrder.ASC);
					// i=0;是涨停当天
					DaliyBasicInfo d1 = dailyList.get(1);// 第二天
					double topPrice = CurrencyUitl.topPrice(d1.getYesterdayPrice(), false);
					if (topPrice > d1.getOpen()) {
						t1.setBuyed(true);
						DaliyBasicInfo d2 = dailyList.get(2);// 第三天开盘价或者收盘价>第二天集合竞价的买入价
						if (d2.getOpen() > d1.getOpen() || d2.getClose() > d1.getOpen()) {
							t1.setOk(true);
						}
					}
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, t1.getDaliyBasicInfo().getCode(),
							t1.getDaliyBasicInfo().getTrade_date() + "", "");
					e.printStackTrace();
				}
			}

			int total = 0;
			int isok = 0;
			for (TraceSortv1Vo t1 : samples) {
				if (t1.isBuyed()) {
					total++;
					if (t1.isOk()) {
						isok++;
					}
					log.info(t1.toDetailStr());
				}
			}
			if (total > 0) {
				WxPushUtil.pushSystem1(startDate + " " + endDate + "样本数量:" + samples.size() + ",成功买入次数:" + total
						+ ",盈利次数:" + isok + ",盈利:" + CurrencyUitl.roundHalfUp(isok / Double.valueOf(total)) + "%");
			} else {
				WxPushUtil.pushSystem1(startDate + " " + endDate + "样本数量:" + samples.size() + ",无成功买入样例");
			}
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1(startDate + " " + endDate + "样本出错！");
		}
	}

}
