package com.stable.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ListenableFuture;
import com.stable.constant.Constant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsStockBaseInfoDao;
import com.stable.job.MyCallable;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.spider.xq.StockListSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 基本信息
 */
@Service
@Log4j2
public class StockBasicService {

	public static final String NO = "no";
	@Autowired
	private EsStockBaseInfoDao esStockBaseInfoDao;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private StockListSpider stockListSpider;

	// @Autowired
	// private DbStockBaseInfoDao dbStockBaseInfoDao;

	public String getCodeName2(String code) {
		return getCodeName(code) + "(" + code + ")";
	}

	public String getCodeName(String code) {
		StockBaseInfo name = getCode(code);
		if (name == null) {
			log.warn("未找到code={},新股或者已退市", code);
			return code;
		}
		return name.getName();
	}

	public StockBaseInfo getCode(String code) {
		String json = redisUtil.get(code);
		if (StringUtils.isNotBlank(json)) {
			StockBaseInfo old = JSON.parseObject(json, StockBaseInfo.class);
			return old;
		}
		Optional<StockBaseInfo> db = esStockBaseInfoDao.findById(code);
		if (!db.isPresent()) {
			StockBaseInfo sb = new StockBaseInfo();
			sb.setCode(NO);
			sb.setName(NO);
			return sb;
		}
		return db.get();
	}

	public ListenableFuture<Object> jobSynStockListV2() {
		return TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.STOCK_LIST, RunCycleEnum.WEEK) {
					public Object mycall() {
						jobSynStockListV2Dir();
						return null;
					}
				});
	}

	public void jobSynStockListV2Dir() {
		try {
			log.info("同步股票列表[started]");
			// System.err.println(array.toJSONString());
			List<StockBaseInfo> list = stockListSpider.getStockList();// 零时数据
			int cnt = 0;
			if (list != null && list.size() > 0) {

				for (StockBaseInfo t : list) {
					StockBaseInfo update = this.getCode(t.getCode());
					update.setCode(t.getCode());
					update.setName(t.getName());
					update.setList_date(t.getList_date());
					update.setList_status(t.getList_status());
					update.setMarket(t.getMarket());

					save(update);
				}

				saveAll(list);
				cnt = list.size();
			} else {
				MsgPushServer.pushToSystem("同步股票列表异常,获取0条数据");
			}
			log.info("同步股票列表[end],cnt=" + cnt);
		} catch (Exception e) {
			e.printStackTrace();
			MsgPushServer.pushToSystem("同步新股股票列表异常");
			throw e;
		}
	}

	public void saveAll(List<StockBaseInfo> list) {
		if (list != null && list.size() > 0) {
			esStockBaseInfoDao.saveAll(list);
		}
	}

	public void synDwcfCompanyType(String code, int dfcwCompnayType) {
		StockBaseInfo b = this.getCode(code);
		b.setDfcwCompnayType(dfcwCompnayType);
		save(b);
	}

	public void synName(String code, String name) {
		StockBaseInfo b = this.getCode(code);
		b.setName(name);
		save(b);
	}

	public void deleteTuiShi() {
		List<StockBaseInfo> removelist = new LinkedList<StockBaseInfo>();
		Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
		while (it.hasNext()) {
			StockBaseInfo e = it.next();
			if (isTuiShi(e.getName())) {
				removelist.add(e);// 已退市
				log.info("删除退市股票：" + e.getCode() + " " + e.getName());
			}
		}
		if (removelist.size() > 0) {
			for (StockBaseInfo s : removelist) {
				redisUtil.del(s.getCode());
			}
			esStockBaseInfoDao.deleteAll(removelist);

			log.info("退市股票数量：" + removelist.size());
		}
	}

	public boolean isTuiShi(String name) {
		return (name.startsWith(Constant.TUI_SHI) || name.endsWith(Constant.TUI_SHI));
	}

	// 排除3:排除退市股票&ST
	public boolean is_ST_And_TuiShi(String name, String code) {
		if (name.startsWith(Constant.TUI_SHI) || name.endsWith(Constant.TUI_SHI) || name.contains(Constant.ST)) {
			log.info("ST或退市,{},{}", code, name);
			return true;
		}
		return false;
	}

	public void synBaseStockInfoCircZb(String code, double circZb) {
		if (circZb > 0) {
			StockBaseInfo b = this.getCode(code);
			b.setCircZb(CurrencyUitl.roundHalfUp(circZb));
			save(b);
		}
	}

	private void save(StockBaseInfo b) {
		if (!b.getCode().equals(NO)) {
			redisUtil.set(b.getCode(), b);
			esStockBaseInfoDao.save(b);
		}
	}

	public void synBaseStockInfo(StockBaseInfo base) {
		save(base);
		log.info("syn stock code:{}", base);
	}

	public synchronized void recashToRedis() {
		List<StockBaseInfo> list = getAllOnStatusListWithSort();
		for (StockBaseInfo b : list) {
			redisUtil.set(b.getCode(), b);
		}
	}

	public synchronized List<StockBaseInfo> getAllOnStatusListWithOutSort() {
		return getAllOnStatusListWithSort(false);
	}

	public synchronized List<StockBaseInfo> getAllOnStatusListWithSort() {
		return getAllOnStatusListWithSort(true);
	}

//	public synchronized List<StockBaseInfo> getAllList() {
//		List<StockBaseInfo> listt = new LinkedList<StockBaseInfo>();
//		Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
//		while (it.hasNext()) {
//			StockBaseInfo e = it.next();
//			if (isHuShenCode(e.getCode())) {// 排除4,8开头的
//				// list.add(e);
//				listt.add(e);
//			}
//		}
//		return listt;
//	}

	private synchronized List<StockBaseInfo> getAllOnStatusListWithSort(boolean issort) {
		List<StockBaseInfo> copy = new LinkedList<StockBaseInfo>();
		Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
		// List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
		while (it.hasNext()) {
			StockBaseInfo e = it.next();
			// list_status='L'
			if (Constant.CODE_ON_STATUS.equals(e.getList_status()) && isHuShenCode(e.getCode())) {// 排除4,8开头的
				copy.add(e);
			}
		}
		if (issort) {
			Collections.sort(copy, sort);
		}
		return copy;
	}

	public synchronized List<StockBaseInfo> nonHhuShen() {
		List<StockBaseInfo> copy = new LinkedList<StockBaseInfo>();
		Iterator<StockBaseInfo> it = esStockBaseInfoDao.findAll().iterator();
		// List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
		while (it.hasNext()) {
			StockBaseInfo e = it.next();
			// list_status='L'
			if (!isHuShenCode(e.getCode())) {// 排除4,8开头的
				copy.add(e);
			}
		}
		return copy;
	}

	// 沪深股票，0，6，3开头的
	public boolean isHuShenCode(String code) {
		return code.startsWith("0") || code.startsWith("3") || code.startsWith("6");
	}

	private Comparator<StockBaseInfo> sort = new Comparator<StockBaseInfo>() {
		@Override
		public int compare(StockBaseInfo o1, StockBaseInfo o2) {
			return o1.getCode().compareTo(o2.getCode());
		}
	};

	/**
	 * 上市年限检查-是否超过N年
	 */
	public boolean onlinePreYearChk(String code, int preYearChk) {
		String json = redisUtil.get(code);
		if (StringUtils.isBlank(json)) {
			return false;
		}
		StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
		String listDate = base.getList_date();
		if (Integer.valueOf(listDate) > preYearChk) {// ----preYearChk<listDate
			return false;
		}
		return true;
	}

	public boolean xiaoshizhi(StockBaseInfo s) {
		if (s.getCircMarketVal() > 0) {
			if (s.getCircMarketVal() < Constant.YI_200) {
				return true;
			}
			return false;
		}
		// 缓存无数据，计算
		DaliyBasicInfo2 db = daliyBasicHistroyService.queryLastest(s.getCode());
		if (db != null && db.getCircMarketVal() > 0) {
			if (db.getCircMarketVal() < Constant.YI_200) {
				return true;
			} else {
				return false;
			}
		}
		// 默认true
		return true;
	}

	public String getMaketcode(String s) {
		if (s.contains("上海")) {
			return "1";
		}
		if (s.contains("深圳")) {
			return "2";
		}
		if (s.contains("北京")) {
			return "3";
		}
		return "0";
	}

	public String getMaketcode2(String s) {
		if (s.contains("SH")) {
			return "1";
		}
		if (s.contains("SZ")) {
			return "2";
		}
		if (s.contains("BJ")) {
			return "3";
		}
		return "0";
	}
}
