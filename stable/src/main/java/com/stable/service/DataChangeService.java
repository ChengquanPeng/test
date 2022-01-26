package com.stable.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;

@Service
public class DataChangeService {

	private Map<String, Double> m1 = new ConcurrentHashMap<String, Double>();

	public void putPeTtmData(List<DaliyBasicInfo2> upd) {
		m1 = new ConcurrentHashMap<String, Double>();
		for (DaliyBasicInfo2 db : upd) {
			m1.put(db.getCode(), db.getPeTtm());
		}
	}

	public void getPeTtmData(String code, CodeBaseModel2 newOne, CodeBaseModel2 oldOne) {
		Double d = m1.get(code);
		if (d != null) {
			newOne.setPettm(d);
		} else if (oldOne.getPettm() != 0.0) {
			newOne.setPettm(oldOne.getPettm());
		}
	}
}
