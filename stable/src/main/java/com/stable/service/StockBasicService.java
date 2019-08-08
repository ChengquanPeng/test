package com.stable.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.db.dao.DbStockBaseInfoDao;
import com.stable.es.dao.EsStockBaseInfoDao;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.StockBaseInfo;

@Service
public class StockBasicService {

	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsStockBaseInfoDao esStockBaseInfoDao;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private DbStockBaseInfoDao dbStockBaseInfoDao;

	//@PostConstruct
	public void synStockList() {
		JSONArray array = tushareSpider.getStockCodeList();
		// System.err.println(array.toJSONString());
		for (int i = 0; i < array.size(); i++) {
			StockBaseInfo base = new StockBaseInfo(array.getJSONArray(i));
			synBaseStockInfo(base);
		}
	}

	public void synBaseStockInfo(StockBaseInfo base) {
		esStockBaseInfoDao.save(base);
		redisUtil.set(base.getCode(), base);
		dbStockBaseInfoDao.saveOrUpdate(base);
	}
	
	public List<StockBaseInfo> getAllOnStatusList(){
		return dbStockBaseInfoDao.getListWithOnStauts();
	}
}
