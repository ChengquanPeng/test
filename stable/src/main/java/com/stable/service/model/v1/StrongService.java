package com.stable.service.model.v1;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
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

	private Map<Integer, Double> getIndexMap(String code, int chkDate, int startedDate) {
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

	public List<DaliyBasicInfo> checkStrong(ModelV1 mv1) {
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
		int volIndex = 0;
		// 3天连续放量
		if (d3.getVol() > d2.getVol() && d2.getVol() > d1.getVol()) {
			volIndex += 5;
		}
		// 突然放量
		if (d3.getVol() > d2.getVol()) {
			long half = d2.getVol() / 2;
			if (d3.getVol() >= (d2.getVol() + half)) {
				volIndex += 5;
			}
		}
		// 流通市值50亿 && 流通换手率超过5%
		if (d3.getCirc_mv() < 500000) {
			if (d3.getTurnover_rate_f() >= 4.9) {
				volIndex += 5;
			}
			// 流通市值100亿 && 流通换手率超过4%
		} else if (d3.getCirc_mv() < 1000000) {
			if (d3.getTurnover_rate_f() >= 3.9) {
				volIndex += 5;
			}
		}
		// 换手率高-过滤掉
		if (d3.getTurnover_rate_f() >= 30.0) {
			volIndex = -100;
			mv1.setVolIndex(volIndex);
			return dailyList;
		}
		mv1.setVolIndex(volIndex);
		// ======= 短线--交易量指标 =======
		// ======= 短线--价格指标 =======
		int sortPriceIndex = 0;
		double high = Stream.of(d3.getHigh(), d2.getHigh(), d1.getHigh()).max(Double::compare).get();
		double low = Stream.of(d3.getLow(), d2.getLow(), d1.getLow()).max(Double::compare).get();
		double chkLine20 = CurrencyUitl.topPrice20(low);
		log.info("3 days code={},date={},high={},low={},chkLine20={}", last.getCode(), last.getTrade_date(), high, low,
				chkLine20);
		if (chkLine20 > high) {// 振浮在20%以内
			sortPriceIndex = 10;
			if (high < CurrencyUitl.topPrice(low, false)) {// 振浮在10%以内
				sortPriceIndex = 6;
				if (high < CurrencyUitl.topPrice(low, true)) {// 振浮在5%以内
					sortPriceIndex = 2;
				}
			}
		} else {
			// 3天振幅超过30%
			if (high >= CurrencyUitl.topPrice30(low)) {
				sortPriceIndex = -100;
				mv1.setSortPriceIndex(sortPriceIndex);
				return dailyList;
			}
		}
		// 大盘上涨:当日价格收跌或者收盘价较最高价跌5%，剔除短线资格; 大盘下跌: 收盘涨跌幅强于大盘涨跌幅可考虑观察。
		if (cache.get(d3.getTrade_date()) >= 0.0) {
			if (d3.getTodayChangeRate() < 0 || d3.getHigh() > CurrencyUitl.topPrice(d3.getClose(), true)) {
				sortPriceIndex = -100;
				mv1.setSortPriceIndex(sortPriceIndex);
				return dailyList;
			}
		}
		mv1.setSortPriceIndex(sortPriceIndex);
		// ======= 短线--价格指标 =======

		// check-5
		int sortStrong = 0;
		index = 5;
		base = 0d;
		stock = 0d;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = dailyList.get(i);
			double stdTodayChangeRate = cache.get(db.getTrade_date());
			if (db.getTodayChangeRate() > stdTodayChangeRate) {
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
		if (stock > base) {
			sortStrong++;
		} else {
			sortStrong--;
		}
		if (sortStrong > 0) {
			sortStrong += 5;// 提高权重
		}
		mv1.setSortStrong(sortStrong);
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
