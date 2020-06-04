package com.stable.service.model.data;

import java.util.Map;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
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

	public synchronized Map<Integer, Double> getIndexMap(String code, int chkDate, int startedDate) {
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
}
