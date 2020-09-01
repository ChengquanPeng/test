package com.stable.service.trace;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.enums.StockAType;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TickDataService;
import com.stable.service.model.data.LineVol;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.TickDataUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TickData;
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
					JSONObject data = tushareSpider.getStockDaliyBasic(s.getTs_code(), null, startDate, endDate);
					JSONArray array2 = data.getJSONArray("items");
					if (array2 != null && array2.size() > 0) {
						for (int ij = 0; ij < array2.size(); ij++) {
							DaliyBasicInfo d2 = new DaliyBasicInfo(array2.getJSONArray(ij));
							log.info("TraceSortv1Vo procssing code={},date={}", d2.getCode(), d2.getTrade_date());
							double topPrice = CurrencyUitl.topPrice(d2.getYesterdayPrice(), false);// 涨停价格
							if (topPrice == d2.getClose()) {// 涨停的票
								if (d2.getOpen() == topPrice) {// 一字板
									continue;
								}
								if (d2.getOpen() > CurrencyUitl.topPrice(d2.getYesterdayPrice(), true)) {// 开盘超过5%
									continue;
								}

								List<DaliyBasicInfo> dailyList = daliyBasicHistroyService.queryListByCode(s.getCode(),
										0, d2.getTrade_date(), queryPage, SortOrder.DESC);
								LineVol lineVol = new LineVol(dailyList);
								if (!lineVol.isShortVol()) {// 缩量?
									continue;
								}
								// 未开板
								List<String> lines = tickDataService.getFromTushare(d2.getCode(), d2.getTrade_date());
								if (lines != null && lines.size() > 10) {
									boolean checkPriceOpen = true;
									List<TickData> tds = new LinkedList<TickData>();
									for (String line : lines) {
										tds.add(TickDataUitl.getDataObjectFromTushare(line));
									}

									// 第一次涨停价格
									TickData firstTopPrice = null;
									for (int i = 0; i < tds.size(); i++) {
										TickData td = tds.get(i);
										if (td.getPrice() == d2.getClose()) {
											firstTopPrice = td;
											break;
										}
									}
									// 涨停之前的5分钟价格
									int chkstime = getBeforeTime(firstTopPrice.getTime());
									int chketime = firstTopPrice.getInttime();
									List<TickData> befor5List = new LinkedList<TickData>();
									for (int i = 0; i < tds.size(); i++) {
										TickData td = tds.get(i);
										if (td.getInttime() >= chkstime && td.getInttime() <= chketime) {
											befor5List.add(td);
										}
										if (td.getInttime() > chketime && td.getPrice() < topPrice) {
											checkPriceOpen = false;
											break;
										}
									}
									// 未开版
									if (!checkPriceOpen) {
										break;
									}

									double min = befor5List.stream().min(Comparator.comparingDouble(TickData::getPrice))
											.get().getPrice();
									if (topPrice > CurrencyUitl.topPrice(min, true)) {// 是否5分钟之内涨停超过5%涨停？
										TraceSortv1Vo tv = new TraceSortv1Vo();
										tv.setDaliyBasicInfo(d2);
										tv.setFirstTopPrice(firstTopPrice);
										samples.add(tv);
										log.info("TraceSortv1Vo get sample:{}", tv);
									}
								}
							}
						}
					}

				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "", "", "");
				}
				ThreadsUtil.thsSleepRandom();
			}

			log.info("获取样本数:" + samples.size());
			for (TraceSortv1Vo t1 : samples) {
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

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat sdf2 = new SimpleDateFormat("HHmmss");
	long diff1 = 5 * 60 * 1000;
	long diff2 = (90 * 60 * 1000) + diff1;

	private int getBeforeTime(String HHmmss) {
		try {
			Date clockInTime = sdf.parse("1970-01-01 " + HHmmss);
			Date nowTime = new Date(clockInTime.getTime() - diff1);
			int r1 = Integer.valueOf(sdf2.format(nowTime));
			if (r1 > 113001 && r1 < 130000) {// 调过午休时间s
				Date nowTime2 = new Date(clockInTime.getTime() - diff2);
				return Integer.valueOf(sdf2.format(nowTime2));
			} else {
				return r1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Integer.valueOf(HHmmss);
	}
}
