package com.stable.service;

import java.util.List;
import java.util.Map;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.spider.tushare.TushareSpider;
import com.stable.vo.StrongVo;
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

	private Map<Integer, Double> getIndexMap(String code, int chkDate) {
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
		JSONArray array = tushareSpider.getIndexDaily(index);
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

	public DaliyBasicInfo checkStrong(ModelV1 mv1, StrongVo sv) {
		String code = mv1.getCode();
		Map<Integer, Double> cache = this.getIndexMap(code, mv1.getDate());

		List<DaliyBasicInfo> list = daliyBasicHistroyService.queryListByCode(code, null, null, queryPage).getContent();
		if (list.size() < 5) {
			log.warn("checkStrong get size<5");
			return null;
		}
		// check-3
		int strongTimes3 = 0;
		int index = 3;
		int strongDef3 = 0;
		double base = 0d;
		double stock = 0d;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = list.get(i);
			if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
				strongTimes3++;
			}
			stock += db.getTodayChangeRate();
			base += cache.get(db.getTrade_date());
		}
		if (stock > base) {
			strongDef3 = 1;
		}
		sv.setStrongTimes3(strongTimes3);
		sv.setStrongDef3(strongDef3);
		// check-5
		int strongTimes5 = 0;
		index = 5;
		int strongDef5 = 0;
		base = 0d;
		stock = 0d;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = list.get(i);
			if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
				strongTimes5++;
			}
			stock += db.getTodayChangeRate();
			base += cache.get(db.getTrade_date());
		}
		if (stock > base) {
			strongDef5 = 1;
		}
		sv.setStrongTimes5(strongTimes5);
		sv.setStrongDef5(strongDef5);
		// check-10
		if (list.size() < 10) {
			return list.get(list.size() - 1);
		}
		index = 10;
		int strongTimes10 = 0;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = list.get(i);
			if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
				strongTimes10++;
			}
		}
		sv.setStrongTimes10(strongTimes10);
		// check-20
		if (list.size() < 20) {
			return list.get(list.size() - 1);
		}
		index = 20;
		int strongTimes20 = 0;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = list.get(i);
			if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
				strongTimes20++;
			}
		}
		sv.setStrongTimes20(strongTimes20);
		// check-120
		if (list.size() < 120) {
			return list.get(list.size() - 1);
		}
		index = 120;
		int strongTimes120 = 0;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = list.get(i);
			if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
				strongTimes120++;
			}
		}
		sv.setStrongTimes120(strongTimes120);
		// check-250
		index = list.size();
		int strongTimes250 = 0;
		for (int i = 0; i < index; i++) {
			DaliyBasicInfo db = list.get(i);
			if (db.getTodayChangeRate() > cache.get(db.getTrade_date())) {
				strongTimes250++;
			}
		}
		sv.setStrongTimes250(strongTimes250);

		getRes(mv1, sv);
		return list.get(list.size() - 1);
	}

	private void getRes(ModelV1 mv1, StrongVo sv) {
		// 短期强势
		int sortStrong = 0;
		boolean s3 = (sv.getStrongDef3() > 0 && sv.getStrongTimes3() > 0);
		boolean s5 = (sv.getStrongDef5() > 0 && sv.getStrongTimes5() > 0);
		if (s3 || s5) {
			sortStrong = 1;
		}
		if (s3 && s5) {
			sortStrong = 3;
		}
		// 中期强势
		boolean s10 = (sv.getStrongTimes10() > 0);
		boolean s20 = (sv.getStrongTimes20() > 0);
		int midStrong = 0;
		if (s10 || s20) {
			midStrong = 1;
		}
		if (s10 && s20) {
			midStrong = 3;
		}
		// 长期强势
		boolean s120 = (sv.getStrongTimes120() > 0);
		boolean s250 = (sv.getStrongTimes250() > 0);
		int lngStrong = 0;
		if (s120 || s250) {
			lngStrong = 1;
		}
		if (s120 && s250) {
			lngStrong = 3;
		}

		mv1.setSortStrong(sortStrong);
		mv1.setMidStrong(midStrong);
		mv1.setLngStrong(lngStrong);
	}
}
