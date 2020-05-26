package com.stable.service.model.v1;

import java.util.List;
import java.util.Map;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.ModelV1context;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class StrongService {

	@Autowired
	private TushareSpider tushareSpider;

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

//	"000001.SH","上证综指",
//	"399001.SZ","深证成指",
//	"399005.SZ","中小板指",
//	"399006.SZ","创业板指",
	private final String INDEX_SH = "000001.SH";
	private final String INDEX_SZ1 = "399001.SZ";
	private final String INDEX_SZ2 = "399005.SZ";
	private final String INDEX_SZ3 = "399006.SZ";

	private String getIndex(String code) {
		if (code.startsWith("6")) {
			return INDEX_SH;
		} else if (code.startsWith("000")) {
			return INDEX_SZ1;
		} else if (code.startsWith("001")) {
			return INDEX_SZ1;
		} else if (code.startsWith("002")) {
			return INDEX_SZ2;
		} else if (code.startsWith("3")) {
			return INDEX_SZ3;
		}
		return null;
	}

	private Map<Integer, Double> M_SH = new ConcurrentHashMap<Integer, Double>();
	private Map<Integer, Double> M_SZ1 = new ConcurrentHashMap<Integer, Double>();
	private Map<Integer, Double> M_SZ2 = new ConcurrentHashMap<Integer, Double>();
	private Map<Integer, Double> M_SZ3 = new ConcurrentHashMap<Integer, Double>();

	private synchronized Map<Integer, Double> getIndexMap(String code, int chkDate, int startedDate) {
		String index = this.getIndex(code);
		Map<Integer, Double> cache = null;
		switch (index) {
		case INDEX_SH:
			cache = M_SH;
			break;
		case INDEX_SZ1:
			cache = M_SZ1;
			break;
		case INDEX_SZ2:
			cache = M_SZ2;
			break;
		case INDEX_SZ3:
			cache = M_SZ3;
			break;
		}
		Double id = cache.get(chkDate);
		if (id != null) {
			return cache;
		}
		JSONArray array = tushareSpider.getIndexDaily(index, startedDate);
		if (array == null || array.size() <= 0) {
			log.warn("未获取到tushareSpider.getIndexDaily(index)={}", index);
			return null;
		} else {
			for (int i = 0; i < array.size(); i++) {
				JSONArray arr = array.getJSONArray(i);
				// KEY:trade_date,VAL:pct_chg
				cache.put(Integer.valueOf(arr.getString(0)), Double.valueOf(arr.getString(1)));
			}
			return cache;
		}
	}

	private final EsQueryPageReq queryPage = new EsQueryPageReq(250);

	public List<DaliyBasicInfo> checkStrong(ModelV1 mv1, ModelV1context wv) {
		String code = mv1.getCode();
		List<DaliyBasicInfo> dailyList = daliyBasicHistroyService
				.queryListByCodeForModel(code, mv1.getDate(), queryPage).getContent();
		if (dailyList.size() < 5) {
			log.warn("checkStrong get size<5");
			return null;
		}
		DaliyBasicInfo last = dailyList.get(dailyList.size() - 1);
		log.info("daliy last,code={},date={}", last.getCode(), last.getTrade_date());
		Map<Integer, Double> cache = this.getIndexMap(code, mv1.getDate(), last.getTrade_date());
		// check-3
		int index = 3;
		double base = 0d;
		double stock = 0d;

		DaliyBasicInfo d3 = dailyList.get(0);
		DaliyBasicInfo d2 = dailyList.get(1);
		DaliyBasicInfo d1 = dailyList.get(2);

		// ======= 短线--交易量指标 =======
		// 换手率高-过滤掉
		if (d3.getTurnover_rate_f() >= 30.0) {
			mv1.setVolIndex(-100);
			return dailyList;
		}
		// 大盘上涨，但当日价格收跌剔除短线资格
		if (cache.get(d3.getTrade_date()) >= 0.0 && d3.getTodayChangeRate() < 0) {
			mv1.setVolIndex(-100);
			return dailyList;
		}

		int volIndex = 0;
		// 3天连续放量上涨
		if (d3.getVol() > d2.getVol() && d2.getVol() > d1.getVol() && d3.getClose() > d2.getClose()
				&& d2.getClose() > d1.getClose()) {
			volIndex += 5;
			wv.addDetailDesc("3天连续放量上涨");
		}
		// 突然放量上涨
		if (d3.getClose() > d2.getClose() && d3.getVol() > d2.getVol()) {
			long half = d2.getVol() / 2;
			if (d3.getVol() >= (d2.getVol() + half)) {
				volIndex += 5;
				wv.addDetailDesc("突然放量上涨");
			}
		}
		if (volIndex > 0) {
			// 流通市值50亿 && 流通换手率超过5%
			if (d3.getCirc_mv() < 500000) {
				if (d3.getTurnover_rate_f() >= 4.9) {
					volIndex += 2;
					wv.addDetailDesc("流值50亿-换手率超过5%");
				}
				// 流通市值100亿 && 流通换手率超过4%
			} else if (d3.getCirc_mv() < 1000000) {
				if (d3.getTurnover_rate_f() >= 3.9) {
					volIndex += 2;
					wv.addDetailDesc("流值100亿-换手率超过4%");
				}
			}
		}
		mv1.setVolIndex(volIndex);
		if (volIndex <= 0) {
			return dailyList;
		}
		// ======= 短线--交易量指标 =======

		// check-5
		int sortStrong = 0;
		index = 5;
		base = 0d;
		stock = 0d;
		int days = 0;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = dailyList.get(i);
			double stdTodayChangeRate = cache.get(db.getTrade_date());
			if (db.getTodayChangeRate() > stdTodayChangeRate) {
				days++;
				sortStrong++;
				if (db.getTodayChangeRate() > 0 && stdTodayChangeRate < 0) {
					// 2，大盘下跌时，个股强势基础分+1，个股翻红时：大盘下跌0-0.5%以内+2，大盘下跌0.5%-1%以内+3，大盘下跌1-2%之间+5，大盘下跌2%以上+7，
					if (stdTodayChangeRate >= -0.5) {
						sortStrong += 2;
					} else if (stdTodayChangeRate >= -1.0) {
						sortStrong += 3;
					} else if (stdTodayChangeRate >= -2.0) {
						sortStrong += 5;
					} else {
						sortStrong += 7;
					}
				}
			}
			stock += db.getTodayChangeRate();
			base += cache.get(db.getTrade_date());
		}
		if (sortStrong > 0) {
			if (stock > base) {
				sortStrong++;
				sortStrong += 5;// 提高权重
				wv.addDetailDesc("5天对比大盘强势次数:" + days);
			} else {
				sortStrong = 0;
			}
		}
		mv1.setSortStrong(sortStrong);
		if (sortStrong <= 0) {
			return dailyList;
		}

		// 排除上影线
		double diff = d3.getHigh() - d3.getLow();
		double half = diff / 2;
		double mid = CurrencyUitl.roundHalfUp(half) + d3.getLow();
		if (d3.getClose() <= mid) {
			mv1.setSortStrong(0);
			return dailyList;
		}

		/*
		 * // check-10 if (list.size() < 10) { return list.get(list.size() - 1); } index
		 * = 10; int strongTimes10 = 0; for (int i = 0; i < index; i++) { DaliyBasicInfo
		 * db = list.get(i); if (db.getTodayChangeRate() >
		 * cache.get(db.getTrade_date())) { strongTimes10++; } }
		 * sv.setStrongTimes10(strongTimes10); // check-20 if (list.size() < 20) {
		 * return list.get(list.size() - 1); } index = 20; int strongTimes20 = 0; for
		 * (int i = 0; i < index; i++) { DaliyBasicInfo db = list.get(i); if
		 * (db.getTodayChangeRate() > cache.get(db.getTrade_date())) { strongTimes20++;
		 * } } sv.setStrongTimes20(strongTimes20); // check-120 if (list.size() < 120) {
		 * return list.get(list.size() - 1); } index = 120; int strongTimes120 = 0; for
		 * (int i = 0; i < index; i++) { DaliyBasicInfo db = list.get(i); if
		 * (db.getTodayChangeRate() > cache.get(db.getTrade_date())) { strongTimes120++;
		 * } } sv.setStrongTimes120(strongTimes120); // check-250 index = list.size();
		 * int strongTimes250 = 0; for (int i = 0; i < index; i++) { DaliyBasicInfo db =
		 * list.get(i); if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
		 * strongTimes250++; } } sv.setStrongTimes250(strongTimes250);
		 */
		return dailyList;
	}
}
