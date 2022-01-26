package com.stable.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.stable.vo.bus.DaliyBasicInfo2;

@Service
public class DataChangeService {

	private Map<String, Double> m1 = new ConcurrentHashMap<String, Double>();

	public void setPeTtmData(List<DaliyBasicInfo2> upd) {
		m1 = new ConcurrentHashMap<String, Double>();
		for (DaliyBasicInfo2 db : upd) {
			m1.put(db.getCode(), db.getPeTtm());
		}
	}

	public Double getPeTtmData(String code) {
		Double d = m1.get(code);
		if (d == null) {
			return 0.0;
		}
		return d;
	}
}
