package com.stable.service.model.data;

import java.util.Map;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.enums.StockAType;
import com.stable.spider.tushare.TushareSpider;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class StrongService {

	@Autowired
	private TushareSpider tushareSpider;

//	"000001.SH","上证综指",
//	"399001.SZ","深证成指",
//	"399005.SZ","中小板指",
//	"399006.SZ","创业板指",
	private int defaultDate = 20100101;
	private final String INDEX_SH = "000001.SH";
	private final String INDEX_SZ_MAIN = "399001.SZ";
	private final String INDEX_SZ_CYB = "399006.SZ";
	private final String INDEX_SH_KCB = "000688.SH";

	private final String SH_START = StockAType.SHM.getStartWith();
	private final String SH_KCB_START = StockAType.KCB.getStartWith();
	private final String SZ_START = StockAType.SZM.getStartWith();
	private final String SZ_CYB_START = StockAType.CYB.getStartWith();

	private String getIndex(String code) {
		if (code.startsWith(SH_START)) {
			return INDEX_SH;
		} else if (code.startsWith(SZ_START)) {
			return INDEX_SZ_MAIN;
		} else if (code.startsWith(SZ_CYB_START)) {
			return INDEX_SZ_CYB;
		} else if (code.startsWith(SH_KCB_START)) {
			return INDEX_SH_KCB;
		}
		return null;
	}

	private Map<Integer, Double> M_SH_MAIN = new ConcurrentHashMap<Integer, Double>();
	private Map<Integer, Double> M_SZ_MAIN = new ConcurrentHashMap<Integer, Double>();
	private Map<Integer, Double> M_SZ_CYB = new ConcurrentHashMap<Integer, Double>();
	private Map<Integer, Double> M_SH_KCB = new ConcurrentHashMap<Integer, Double>();

	// == 大盘涨跌 ==
	public boolean checkMarketPrice(int mp, String code, int chkDate) {
		if (mp == 0) {
			return true;
		}
		double dateRate = getIndexPrice(code, chkDate);
		if (mp == 1) {
			return dateRate > 0;
		}
		return dateRate < 0;
	}
	// == 大盘涨跌 ==

	public Double getIndexPrice(String code, int chkDate) {
		return getIndexMap(code, chkDate).get(chkDate);
	}

	public Map<Integer, Double> getIndexMap(String code, int chkDate) {
		String index = this.getIndex(code);
		Map<Integer, Double> cache = null;
		switch (index) {
		case INDEX_SH:
			cache = M_SH_MAIN;
			break;
		case INDEX_SZ_MAIN:
			cache = M_SZ_MAIN;
			break;
		case INDEX_SZ_CYB:
			cache = M_SZ_CYB;
			break;
		case INDEX_SH_KCB:
			cache = M_SH_KCB;
			break;
		}
		Double id = cache.get(chkDate);
		if (id != null) {
			return cache;
		}
		int startedDate = chkDate < defaultDate ? chkDate : defaultDate;
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
}
