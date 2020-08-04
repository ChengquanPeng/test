package com.stable.service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsStockBaseInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.WxPushUtil;
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
	private List<StockBaseInfo> LOCAL_ALL_ONLINE_LIST = new CopyOnWriteArrayList<StockBaseInfo>();

	// 只初始化一次，历史数据包含已下市股票
	private boolean initedOneTime = true;

	public String getCodeName(String code) {
		String name = CODE_NAME_MAP_LOCAL_HASH.get(code);
		if (name == null) {
			loadAllLocalHash();
			name = CODE_NAME_MAP_LOCAL_HASH.get(code);
			if (name == null) {
				try {
					if (initedOneTime) {
						this.jobSynStockList().get();
						initedOneTime = false;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				name = CODE_NAME_MAP_LOCAL_HASH.get(code);
				if (StringUtils.isBlank(name)) {
					log.warn("已下市股票,code：{}", code);
					CODE_NAME_MAP_LOCAL_HASH.put(code, "已下市");
					return "";
				}
			}
		}
		return name;
	}

	private final Semaphore semap = new Semaphore(1);

	public void jobSynStockListAfterUpdateStatus() {

	}

	public static void main(String[] args) {
	}

	public ListenableFuture<Object> jobSynStockList(boolean isJob) {
		try {
			semap.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.STOCK_LIST, RunCycleEnum.WEEK) {
					public Object mycall() {
						try {
							Long batchNo = System.currentTimeMillis();
							log.info("同步股票列表[started]");
							JSONArray array = tushareSpider.getStockCodeList();
							// System.err.println(array.toJSONString());
							List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
							for (int i = 0; i < array.size(); i++) {
								StockBaseInfo base = new StockBaseInfo(array.getJSONArray(i), batchNo);
								synBaseStockInfo(base);
								list.add(base);
							}
							int cnt = 0;
							if (list != null) {
								esStockBaseInfoDao.saveAll(list);
								cnt = list.size();
							}
							List<StockBaseInfo> removelist = new LinkedList<StockBaseInfo>();
							if (isJob) {
								Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
								while (it.hasNext()) {
									StockBaseInfo e = it.next();
									if ("L".equals(e.getList_status()) && (e.getUpdBatchNo() == null
											|| e.getUpdBatchNo().longValue() != batchNo.longValue())) {
										removelist.add(e);
										e.setList_status("off");// 已退市
										log.info("删除异常股票:{}", e);
									}
								}
								if (removelist.size() > 0) {
									esStockBaseInfoDao.saveAll(removelist);
								}
							}
							log.info("同步股票列表[end]");
							LOCAL_ALL_ONLINE_LIST.clear();// 清空缓存
							WxPushUtil.pushSystem1("同步股票列表完成！记录条数=[" + cnt + "],异常股票数:" + removelist.size());
							return null;
						} finally {
							semap.release();
						}
					}
				});
	}

	public ListenableFuture<Object> jobSynStockList() {
		return jobSynStockList(false);
	}

	public void synBaseStockInfo(StockBaseInfo base) {
		// esStockBaseInfoDao.save(base);
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

	public synchronized List<StockBaseInfo> getAllOnStatusList() {
		if (LOCAL_ALL_ONLINE_LIST.isEmpty()) {
			Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
			// List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
			while (it.hasNext()) {
				StockBaseInfo e = it.next();
				// list_status='L'
				if ("L".equals(e.getList_status())) {
					// list.add(e);
					LOCAL_ALL_ONLINE_LIST.add(e);
				}
			}
		}
		return LOCAL_ALL_ONLINE_LIST;
		// return dbStockBaseInfoDao.getListWithOnStauts();
	}

	/**
	 * 上市超一年
	 */
	public boolean online1Year(String code) {
		getCodeName(code);// 同步code
		StockBaseInfo base = JSON.parseObject(redisUtil.get(code), StockBaseInfo.class);
		String listDate = base.getList_date();
		Integer year = Integer.valueOf(listDate.substring(0, 4));
		Integer end = Integer.valueOf((year + 1 + "") + listDate.substring(4));
		if (end <= Integer.valueOf(DateUtil.getTodayYYYYMMDD())) {
			return true;
		}
		return false;
	}
}
