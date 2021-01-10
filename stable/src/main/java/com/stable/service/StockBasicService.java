package com.stable.service;

import java.util.Collections;
import java.util.Comparator;
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
import com.stable.spider.ths.ThsPlateSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 基本信息
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
	@Autowired
	private ThsPlateSpider thsPlateSpider;
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

	public StockBaseInfo getCode(String code) {
		String json = redisUtil.get(code);
		if (StringUtils.isNotBlank(json)) {
			StockBaseInfo old = JSON.parseObject(json, StockBaseInfo.class);
			return old;
		}
		return new StockBaseInfo();
	}

	private final Semaphore semap = new Semaphore(1);

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
								synBaseStockInfo(base, false);
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
								// 更新同花顺
								thsPlateSpider.fetchAll(false);
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

	public void synBaseStockInfo(StockBaseInfo base, boolean updateTHSinfo) {
		// esStockBaseInfoDao.save(base);

		if (!updateTHSinfo) {// 部门字段来自同花顺
			String json = redisUtil.get(base.getCode());
			if (StringUtils.isNotBlank(json)) {
				StockBaseInfo old = JSON.parseObject(json, StockBaseInfo.class);
				base.setThsIndustry(old.getThsIndustry());
				base.setThsLightspot(old.getThsLightspot());
				base.setThsMainBiz(old.getThsMainBiz());
				base.setOldName(old.getOldName());
				base.setWebSite(old.getWebSite());
			}
		}

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
		List<StockBaseInfo> copy = new LinkedList<StockBaseInfo>();
		copy.addAll(LOCAL_ALL_ONLINE_LIST);
		Collections.sort(copy, sort);
		return copy;
		// return dbStockBaseInfoDao.getListWithOnStauts();
	}

	private Comparator<StockBaseInfo> sort = new Comparator<StockBaseInfo>() {
		@Override
		public int compare(StockBaseInfo o1, StockBaseInfo o2) {
			return o1.getCode().compareTo(o2.getCode());
		}
	};

	/**
	 * 上市时间年限chk-1年
	 */
	public boolean online1YearChk(String code, int today) {
		String json = redisUtil.get(code);
		if (StringUtils.isBlank(json)) {
			return false;
		}
		StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
		String listDate = base.getList_date();
		if (today >= DateUtil.getNextYear(Integer.valueOf(listDate))) {
			return true;
		}
		return false;
	}

	/**
	 * 上市时间年限chk-2年
	 */
	public boolean online2YearChk(String code, int today) {
		String json = redisUtil.get(code);
		if (StringUtils.isBlank(json)) {
			return false;
		}
		StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
		String listDate = base.getList_date();
		if (today >= DateUtil.getNext2Year(Integer.valueOf(listDate))) {
			return true;
		}
		return false;
	}
}
