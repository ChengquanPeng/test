package com.stable.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.es.dao.EsBuyBackInfoDao;
import com.stable.spider.tushare.TushareSpider;
import com.stable.vo.bus.BuyBackInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 回购
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class BuyBackService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;

	@PostConstruct
	public void spiderFinaceHistoryInfo() {
		this.spiderFinaceHistoryInfo(null, null);
	}

	public void spiderFinaceHistoryInfo(String start_date, String end_date) {
		log.info("同步回购公告列表[started],start_date={},end_date={},", start_date, end_date);
		JSONArray array = tushareSpider.getBuyBackList(start_date, end_date);
		// System.err.println(array.toJSONString());
		for (int i = 0; i < array.size(); i++) {
			BuyBackInfo base = new BuyBackInfo(array.getJSONArray(i));
			// if(i==0) {
			buyBackInfoDao.save(base);
			// }
			// System.err.println(base);
		}
		log.info("同步回购公告列表[end]");
	}
}
