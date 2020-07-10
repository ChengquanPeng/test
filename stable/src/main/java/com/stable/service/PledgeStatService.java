package com.stable.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.es.dao.base.EsPledgeStatDao;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.PledgeStat;

import lombok.extern.log4j.Log4j2;

/**
 * 质押
 */
@Service
@Log4j2
public class PledgeStatService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsPledgeStatDao esPledgeStatDao;

	public PledgeStat getLastRecords(String code, int date) {
		return getLastRecords(code, String.valueOf(date));
	}

	public PledgeStat getLastRecords(String code, String date) {
		JSONArray array = tushareSpider.getPledgeStatList(TushareSpider.formatCode(code), date);
		if (array != null && array.size() > 0) {
			log.info("{},{},获取到质押公告记录条数={}", code, date, array.size());
			PledgeStat base = new PledgeStat(array.getJSONArray(0));
			esPledgeStatDao.save(base);
			return base;
		} else {
			log.info("未获取到质押公告");
		}
		return null;
	}

	public PledgeStat getLastRecords(String code) {
		return getLastRecords(code, DateUtil.getTodayYYYYMMDD());
	}
}
