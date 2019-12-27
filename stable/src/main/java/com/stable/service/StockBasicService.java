package com.stable.service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.EsStockBaseInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 基本信息
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class StockBasicService {

	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsStockBaseInfoDao esStockBaseInfoDao;
	@Autowired
	private RedisUtil redisUtil;
	// @Autowired
	// private DbStockBaseInfoDao dbStockBaseInfoDao;

	private ConcurrentHashMap<String, String> CODE_NAME_MAP_LOCAL_HASH = new ConcurrentHashMap<String, String>();

	public String getCodeName(String code) {
		String name = CODE_NAME_MAP_LOCAL_HASH.get(code);
		if (name == null) {
			loadAllLocalHash();
			name = CODE_NAME_MAP_LOCAL_HASH.get(code);
			if (name == null) {
				try {
					this.jobSynStockList().get();
				} catch (Exception e) {
					e.printStackTrace();
				}
				name = CODE_NAME_MAP_LOCAL_HASH.get(code);
			}
		}
		return name;
	}

	private final Semaphore semap = new Semaphore(1);

	public ListenableFuture<Object> jobSynStockList() {
		try {
			semap.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.STOCK_LIST, RunCycleEnum.WEEK) {
					public Object mycall() {
						try {
							log.info("同步股票列表[started]");
							JSONArray array = tushareSpider.getStockCodeList();
							// System.err.println(array.toJSONString());
							for (int i = 0; i < array.size(); i++) {
								StockBaseInfo base = new StockBaseInfo(array.getJSONArray(i));
								synBaseStockInfo(base);
							}
							log.info("同步股票列表[end]");
							return null;
						} finally {
							semap.release();
						}
					}
				});
	}

	public void synBaseStockInfo(StockBaseInfo base) {
		esStockBaseInfoDao.save(base);
		redisUtil.set(base.getCode(), base);
		// dbStockBaseInfoDao.saveOrUpdate(base);
		CODE_NAME_MAP_LOCAL_HASH.put(base.getCode(), base.getName());
		log.info("syn stock code list:{}", base);
	}

	public void loadAllLocalHash() {
		Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
		while (it.hasNext()) {
			StockBaseInfo e = it.next();
			CODE_NAME_MAP_LOCAL_HASH.put(e.getCode(), e.getName());
		}
	}

	public List<StockBaseInfo> getAllOnStatusList() {
		Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
		List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
		while (it.hasNext()) {
			StockBaseInfo e = it.next();
			// list_status='L'
			if ("L".equals(e.getList_status())) {
				list.add(e);
			}
		}
		return list;
		// return dbStockBaseInfoDao.getListWithOnStauts();
	}
}
