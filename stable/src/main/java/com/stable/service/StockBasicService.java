package com.stable.service;

import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.db.dao.DbStockBaseInfoDao;
import com.stable.es.dao.EsStockBaseInfoDao;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class StockBasicService {

	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsStockBaseInfoDao esStockBaseInfoDao;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private DbStockBaseInfoDao dbStockBaseInfoDao;

	public void jobSynStockList() {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				log.info("同步股票列表[started]");
				JSONArray array = tushareSpider.getStockCodeList();
				// System.err.println(array.toJSONString());
				for (int i = 0; i < array.size(); i++) {
					StockBaseInfo base = new StockBaseInfo(array.getJSONArray(i));
					synBaseStockInfo(base);
				}
				log.info("同步股票列表[end]");
				return null;
			}
		});
	}

	public void synBaseStockInfo(StockBaseInfo base) {
		esStockBaseInfoDao.save(base);
		redisUtil.set(base.getCode(), base);
		dbStockBaseInfoDao.saveOrUpdate(base);
		log.info("syn stock code list:{}", base);
	}

	public List<StockBaseInfo> getAllOnStatusList() {
		return dbStockBaseInfoDao.getListWithOnStauts();
	}
}
