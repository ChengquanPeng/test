package com.stable.service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsModelV1Dao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyDao;
import com.stable.es.dao.base.EsTradeHistInfoDaliyNofqDao;
import com.stable.job.MyCallable;
import com.stable.service.model.ImageStrategyListener;
import com.stable.service.model.image.ImageService;
import com.stable.service.trace.SortV6Service;
import com.stable.spider.eastmoney.EastmoneyQfqSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.TasksWorker2nd;
import com.stable.utils.TasksWorker2ndRunnable;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.http.resp.DaliyTradeHistResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 日交易历史
 *
 */
@Service
@Log4j2
public class DaliyTradeHistroyService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsTradeHistInfoDaliyDao esTradeHistInfoDaliyDao;
	@Autowired
	private EsTradeHistInfoDaliyNofqDao esTradeHistInfoDaliyNofqDao;

	@Value("${python.file.market.hist.daily}")
	private String pythonFileName;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private ImageService imageService;
	@Autowired
	private EsModelV1Dao esModelV1Dao;
//	@Autowired
//	private UpModelLineService upLevel1Service;
	@Autowired
	private SortV6Service sortV6Service;

	/**
	 * 手动获取日交易记录（所有）
	 */
	public void manualSpiderDaliyTrade(String code) {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL, code) {
					public Object mycall() {
						String today = DateUtil.getTodayYYYYMMDD();
						redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
						log.info("日期前复权：{}重新获取记录", code);
						String json = redisUtil.get(code);
						if (StringUtils.isNotBlank(json)) {
							StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
							spiderDaliyTradeHistoryInfoFromIPOCenter(code, base.getList_date(), today, 0);
						}
						return null;
					}
				});
	}

	/**
	 * code除权
	 */
	public void removeCacheByChuQuan(String code) {
		redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
		priceLifeService.removePriceLifeCache(code);
	}

	public void imageCheck(JSONArray array, String today) {
		ListenableFuture<?> l = TasksWorker.getInstance().getService().submit(new Runnable() {
			@Override
			public void run() {
				int oneYearChkDate = Integer.valueOf(today);
				String startTime = DateUtil.getTodayYYYYMMDDHHMMSS();
				try {
					ImageStrategyListener sl = new ImageStrategyListener();
					for (int i = 0; i < array.size(); i++) {
						DaliyBasicInfo d = new DaliyBasicInfo(array.getJSONArray(i));
						ModelContext mc = new ModelContext();
						mc.setCode(d.getCode());
						mc.setDate(d.getTrade_date());
						if (stockBasicService.online1YearChk(mc.getCode(), oneYearChkDate)) {
							String str = imageService.checkImg(mc.getCode(), mc.getDate());
							if (StringUtils.isNotBlank(str)) {
								if (str.contains(ImageService.MATCH_L2)) {
//									mv.setImageIndex(2);
								} else {
//									mv.setImageIndex(1);
								}
								mc.setImgResult(str);
								sl.processingModelResult(mc, null, null, null, null);
							}
						} else {
							log.info("Online 不足1年，code={}", mc.getCode());
						}

					}
					sl.fulshToFile();// 存盘
					if (sl.getResultList().size() > 0) {
						esModelV1Dao.saveAll(sl.getResultList());
					}
					log.info("Seq5=>图片模型执行完成。");
					WxPushUtil.pushSystem1("Seq5=> " + today + " 图形模型执行完成！ 开始时间:" + startTime + " 结束时间："
							+ DateUtil.getTodayYYYYMMDDHHMMSS() + ",succ=" + sl.getResultList().size());
				} catch (Exception e) {
					e.printStackTrace();
					ErrorLogFileUitl.writeError(e, e.getMessage(), "", "");
					WxPushUtil.pushSystem1("异常Seq5=> " + today + " 图形模型执行异常！ 开始时间:" + startTime);
				}
			}
		});
		try {
			l.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized int spiderTodayDaliyTrade(String today) {
		String preDate = tradeCalService.getPretradeDate(today);
		try {
			JSONArray array = tushareSpider.getStockDaliyTrade(null, today, null, null);
			if (array == null || array.size() <= 0) {
				log.warn("未获取到日交易记录,tushare,code={}");
				return 0;
			}
			log.info("获取到日交易记录条数={}", array.size());
			CountDownLatch cnt = new CountDownLatch(array.size());
			List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
			List<TradeHistInfoDaliyNofq> listNofq = new LinkedList<TradeHistInfoDaliyNofq>();
			for (int i = 0; i < array.size(); i++) {
				// 1.保存记录
				TradeHistInfoDaliy d = new TradeHistInfoDaliy(array.getJSONArray(i));
				String code = d.getCode();
				int qfqDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
				d.setQfqDate(qfqDate);
				list.add(d);
				TradeHistInfoDaliyNofq nofq = new TradeHistInfoDaliyNofq(array.getJSONArray(i));
				listNofq.add(nofq);

				// 2.是否需要更新缺失记录

				String yyyymmdd = redisUtil.get(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
				if (StringUtils.isBlank(yyyymmdd) || (!preDate.equals(yyyymmdd) && !yyyymmdd.equals(today)
						&& Integer.valueOf(yyyymmdd) < Integer.valueOf(today))) {//
					log.info("代码code:{}重新获取记录->redis-last:{},preDate:{},today:{}", code, yyyymmdd, preDate, today);
					String json = redisUtil.get(d.getCode());
					// 第一次上市或者除权
					StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
					if (base != null) {
						yyyymmdd = base.getList_date();
						String datep = yyyymmdd;
						TasksWorker2nd.add(new TasksWorker2ndRunnable() {
							public void running() {
								try {
									spiderDaliyTradeHistoryInfoFromIPOCenter(d.getCode(), datep, today, 0);
								} finally {
									cnt.countDown();
								}
							}
						});
					} else {
						log.info("代码code:{} 未获取到StockBaseInfo", code);
						cnt.countDown();
					}
				} else {
					log.info("代码:{},不需要重新更新记录,上个交易日期 preDate:{},上次更新日期:{},最后更新日期:{},index={}", code, preDate, yyyymmdd,
							today, i);
					redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
					priceLifeService.checkAndSetPrice(d);
					cnt.countDown();
				}
			}
			try {
				if (!cnt.await(12, TimeUnit.HOURS)) {
					WxPushUtil.pushSystem1("前复权qfq获取超时异常==>日期:" + today);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (list.size() > 0) {
				esTradeHistInfoDaliyDao.saveAll(list);
				esTradeHistInfoDaliyNofqDao.saveAll(listNofq);
				return list.size();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			WxPushUtil.pushSystem1("前复权qfq获取异常，获取日期:" + today);
		}
		return 0;

	}

	// 路由，优先eastmoney
	private boolean spiderDaliyTradeHistoryInfoFromIPOCenter(String code, String startDate, String today,
			int fortimes) {
		priceLifeService.removePriceLifeCache(code);
		if (spiderDaliyTradeHistoryInfoFromIPOEastMoney(code, fortimes)) {
			redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
			return true;
		}
		throw new RuntimeException(code + " " + startDate + " " + today + " 日交易获取前复权错误");
//		if (spiderDaliyTradeHistoryInfoFromIPOTushare(code, startDate, today, fortimes)) {
//			redisUtil.set(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code, today);
//			return true;
//		}
//		return false;
	}

	// 路由，优先eastmoney
	private boolean spiderDaliyTradeHistoryInfoFromIPOCenterNofq(String code, String startDate, String today,
			int fortimes) {
		if (spiderDaliyTradeHistoryInfoFromIPOEastMoneyNofq(code, fortimes)) {
			return true;
		}
		throw new RuntimeException(code + " " + startDate + " " + today + " 日交易获取No复权错误");
//		if (spiderDaliyTradeHistoryInfoFromIPOTushareNofq(code, startDate, today, fortimes)) {
//			return true;
//		}
//		return false;
	}

	private boolean spiderDaliyTradeHistoryInfoFromIPOEastMoney(String code, int fortimes) {
		if (fortimes >= 3) {
			log.warn("EastMoeny 超过最大次数：code：{}，fortimes：{}", code, fortimes);
			return false;
		}
		fortimes++;
		List<TradeHistInfoDaliy> list = null;
		try {
			list = EastmoneyQfqSpider.getQfq(code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null || list.size() <= 0) {
			return spiderDaliyTradeHistoryInfoFromIPOEastMoney(code, fortimes);
		} else {
			esTradeHistInfoDaliyDao.saveAll(list);
		}
		return true;
	}

	private boolean spiderDaliyTradeHistoryInfoFromIPOEastMoneyNofq(String code, int fortimes) {
		if (fortimes >= 3) {
			log.warn("EastMoeny 超过最大次数：code：{}，fortimes：{}", code, fortimes);
			return false;
		}
		fortimes++;
		List<TradeHistInfoDaliyNofq> list = null;
		try {
			list = EastmoneyQfqSpider.getWithOutfq(code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null || list.size() <= 0) {
			return spiderDaliyTradeHistoryInfoFromIPOEastMoneyNofq(code, fortimes);
		} else {
			esTradeHistInfoDaliyNofqDao.saveAll(list);
		}
		return true;
	}

	@SuppressWarnings("unused")
	private boolean spiderDaliyTradeHistoryInfoFromIPOTushareNofq(String code, String startDate, String today,
			int fortimes) {
		if (fortimes >= 10) {
			log.warn("Tushare 超过最大次数：code：{}，startDate：{}，endDate：{}，fortimes：{}", code, startDate, today, fortimes);
			return false;
		}
		fortimes++;
		ThreadsUtil.sleepRandomSecBetween1And30();
//		MarketHistroyVo mh = new MarketHistroyVo();
//		mh.setTs_code(TushareSpider.formatCode(code));
//		mh.setAdj("qfq");
//		mh.setStart_date(startDate);
//		mh.setEnd_date(endDate);
//		mh.setFreq("D");

//		code=sys.argv[1]
//		sdate=sys.argv[2]
//		edate=sys.argv[3]
//		padj=sys.argv[4]
//		pfreq=sys.argv[5]
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + today + " None D";
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，未获取到数据 params：{}", code, params);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
			}
			return false;
		}
		log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，获取到数据 条数：szie:{}，", code, lines.size() - 1);
		TradeHistInfoDaliyNofq last = null;
		List<TradeHistInfoDaliyNofq> list = new LinkedList<TradeHistInfoDaliyNofq>();
		for (String line : lines) {
			TradeHistInfoDaliyNofq d = this.getTradeHistInfoDaliyNofq(line);
			if (d != null) {
				list.add(d);
				// this.tradeHistDaliy.save(d);
				last = d;
				if (list.size() > 2000) {
					esTradeHistInfoDaliyNofqDao.saveAll(list);
					list = new LinkedList<TradeHistInfoDaliyNofq>();
				}
			}
		}
		if (list.size() > 0) {
			esTradeHistInfoDaliyNofqDao.saveAll(list);
		}
		log.warn("不复权-spiderDaliyTradeHistoryInfoFromIPO：code：{}，上市日期startDate：{}，本批当前日期last：{}", code, startDate,
				last);
		if (last != null && !startDate.equals(last.getDate() + "")) {
			return spiderDaliyTradeHistoryInfoFromIPOTushareNofq(code, startDate, last.getDate() + "", fortimes);
		}
		return true;
	}

	@SuppressWarnings("unused")
	private boolean spiderDaliyTradeHistoryInfoFromIPOTushare(String code, String startDate, String today,
			int fortimes) {
		if (fortimes >= 10) {
			log.warn("Tushare 超过最大次数：code：{}，startDate：{}，endDate：{}，fortimes：{}", code, startDate, today, fortimes);
			return false;
		}
		fortimes++;
		ThreadsUtil.sleepRandomSecBetween1And30();
//		MarketHistroyVo mh = new MarketHistroyVo();
//		mh.setTs_code(TushareSpider.formatCode(code));
//		mh.setAdj("qfq");
//		mh.setStart_date(startDate);
//		mh.setEnd_date(endDate);
//		mh.setFreq("D");

//		code=sys.argv[1]
//		sdate=sys.argv[2]
//		edate=sys.argv[3]
//		padj=sys.argv[4]
//		pfreq=sys.argv[5]
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + today + " qfq D";
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，未获取到数据 params：{}", code, params);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
			}
			return false;
		}
		int qfqDate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，获取到数据 条数：szie:{}，", code, lines.size() - 1);
		TradeHistInfoDaliy last = null;
		List<TradeHistInfoDaliy> list = new LinkedList<TradeHistInfoDaliy>();
		for (String line : lines) {
			TradeHistInfoDaliy d = this.getTradeHistInfoDaliy(line);
			if (d != null) {
				d.setQfqDate(qfqDate);
				list.add(d);
				// this.tradeHistDaliy.save(d);
				last = d;
				if (list.size() > 2000) {
					esTradeHistInfoDaliyDao.saveAll(list);
					list = new LinkedList<TradeHistInfoDaliy>();
				}
			}
		}
		if (list.size() > 0) {
			esTradeHistInfoDaliyDao.saveAll(list);
		}
		log.warn("spiderDaliyTradeHistoryInfoFromIPO：code：{}，上市日期startDate：{}，本批当前日期last：{}", code, startDate, last);
		if (last != null && !startDate.equals(last.getDate() + "")) {
			return spiderDaliyTradeHistoryInfoFromIPOTushare(code, startDate, last.getDate() + "", fortimes);
		}
		return true;
	}

	public List<DaliyTradeHistResp> queryListByCodeByWebPage(String code, EsQueryPageReq queryPage) {
		List<DaliyTradeHistResp> res = new LinkedList<DaliyTradeHistResp>();
		List<TradeHistInfoDaliy> list = this.queryListByCode(code, 0, 0, queryPage, SortOrder.DESC);
		if (list != null) {
			for (TradeHistInfoDaliy dh : list) {
				DaliyTradeHistResp resp = new DaliyTradeHistResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<TradeHistInfoDaliy> queryListByCodeWithLastQfq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		int qfqDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
		List<TradeHistInfoDaliy> db = queryListByCode(code, startDate, endDate, queryPage, s);
		boolean needFetch = false;
//		log.info("{},qfqDate={}", code, qfqDate);
//		log.info("db is null =" + (db == null));
//		log.info("db.size =" + (db != null ? db.size() : 0));
//		log.info("queryPage.getPageSize=" + queryPage.getPageSize());
//		log.info("SortOrder=" + s.toString());
		if (db == null || db.size() <= 0 || (queryPage.getPageSize() < 9999 && db.size() != queryPage.getPageSize())) {
			log.info(code + " needFetch=true");
			needFetch = true;
		} else {
			if (qfqDate != 0) {
				for (TradeHistInfoDaliy r : db) {
					if (r.getQfqDate() < qfqDate) {// 存的数据是前复权日期版本小于redis，不是最新的
						log.info("{},TradeHistInfoDaliy date={},QfqDate={}", code, r.getDate(), r.getQfqDate());
						needFetch = true;
						break;
					}
				}
			}
			// 数据是否正确
			if (!needFetch && queryPage.getPageSize() < 9999) {
				if (s == SortOrder.DESC) {
					if (endDate != 0 && db.get(0).getDate() != endDate) {
						log.info(code + " endDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(db.size() - 1).getDate() != startDate) {
						log.info(code + " startDate={},db-last={}", startDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
				} else {
					if (endDate != 0 && db.get(db.size() - 1).getDate() != endDate) {
						log.info(code + " endDate={},db-last={}", endDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(0).getDate() != startDate) {
						log.info(code + " startDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
				}
			}
		}

		if (needFetch) {
			log.info("code={},startDate={},endDate={},queryPage={},SortOrder={}", code, startDate, endDate,
					queryPage.getPageSize(), s.toString());
//			new Exception().printStackTrace();
			String today = DateUtil.getTodayYYYYMMDD();
			String json = redisUtil.get(code);
			if (StringUtils.isNotBlank(json)) {
				StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
				if (spiderDaliyTradeHistoryInfoFromIPOCenter(code, base.getList_date(), today, 0)) {
					return queryListByCode(code, startDate, endDate, queryPage, s);
				}
			}
			return null;
		}
		return db;
	}

	private List<TradeHistInfoDaliy> queryListByCode(String code, int startDate, int endDate, EsQueryPageReq queryPage,
			SortOrder s) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}
		if (endDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").lte(endDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliy> page = esTradeHistInfoDaliyDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<TradeHistInfoDaliyNofq> queryListByCodeWithLastNofq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		List<TradeHistInfoDaliyNofq> db = queryListByCodeNofq(code, startDate, endDate, queryPage, s);
		boolean needFetch = false;
//		log.info("{},qfqDate={}", code, qfqDate);
//		log.info("db is null =" + (db == null));
//		log.info("db.size =" + (db != null ? db.size() : 0));
//		log.info("queryPage.getPageSize=" + queryPage.getPageSize());
//		log.info("SortOrder=" + s.toString());
		if (db == null || db.size() <= 0 || (queryPage.getPageSize() < 9999 && db.size() != queryPage.getPageSize())) {
			log.info(code + " needFetch=true");
			needFetch = true;
		} else {
			// 数据是否正确
			if (queryPage.getPageSize() < 9999) {
				if (s == SortOrder.DESC) {
					if (endDate != 0 && db.get(0).getDate() != endDate) {
						log.info(code + " endDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(db.size() - 1).getDate() != startDate) {
						log.info(code + " startDate={},db-last={}", startDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
				} else {
					if (endDate != 0 && db.get(db.size() - 1).getDate() != endDate) {
						log.info(code + " endDate={},db-last={}", endDate, db.get(db.size() - 1).getDate());
						needFetch = true;
					}
					if (startDate != 0 && db.get(0).getDate() != startDate) {
						log.info(code + " startDate={},db-0={}", endDate, db.get(0).getDate());
						needFetch = true;
					}
				}
			}
		}

		if (needFetch) {
			log.info("Nofq code={},startDate={},endDate={},queryPage={},SortOrder={}", code, startDate, endDate,
					queryPage.getPageSize(), s.toString());
			new Exception().printStackTrace();
			String today = DateUtil.getTodayYYYYMMDD();
			String json = redisUtil.get(code);
			if (StringUtils.isNotBlank(json)) {
				StockBaseInfo base = JSON.parseObject(json, StockBaseInfo.class);
				if (spiderDaliyTradeHistoryInfoFromIPOCenterNofq(code, base.getList_date(), today, 0)) {
					return queryListByCodeNofq(code, startDate, endDate, queryPage, s);
				}
			}
			return null;
		}
		return db;
	}

	private List<TradeHistInfoDaliyNofq> queryListByCodeNofq(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, SortOrder s) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}
		if (endDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").lte(endDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<TradeHistInfoDaliyNofq> page = esTradeHistInfoDaliyNofqDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	private TradeHistInfoDaliyNofq getTradeHistInfoDaliyNofq(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		try {
			TradeHistInfoDaliyNofq d = new TradeHistInfoDaliyNofq();
			String str = line.trim().substring(1);
			String[] fv = str.split(",");
			d.setCode(TushareSpider.removets(fv[0]));
			d.setDate(Integer.valueOf(fv[1]));
			d.setOpen(Double.valueOf(fv[2]));
			d.setHigh(Double.valueOf(fv[3]));
			d.setLow(Double.valueOf(fv[4]));
			d.setClosed(Double.valueOf(fv[5]));
			d.setYesterdayPrice(Double.valueOf(fv[6]));
			d.setTodayChange(Double.valueOf(fv[7]));
			d.setTodayChangeRate(Double.valueOf(fv[8]));
			d.setVolume(Double.valueOf(fv[9]));
			try {
				d.setAmt(Double.valueOf(fv[10]));
			} catch (NumberFormatException e) {
				d.setAmt(Double.valueOf(0));
			}
			d.setId();
			return d;
		} catch (Exception e) {
			// ErrorLogFileUitl.writeError(e, "日K数据错误", "原始数据", line);
			log.info("日K数据错误,原始数据:" + line);
			return null;
//			throw new RuntimeException(e);
		}
	}

	private TradeHistInfoDaliy getTradeHistInfoDaliy(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		try {
			TradeHistInfoDaliy d = new TradeHistInfoDaliy();
			String str = line.trim().substring(1);
			String[] fv = str.split(",");
			d.setCode(TushareSpider.removets(fv[0]));
			d.setDate(Integer.valueOf(fv[1]));
			d.setOpen(Double.valueOf(fv[2]));
			d.setHigh(Double.valueOf(fv[3]));
			d.setLow(Double.valueOf(fv[4]));
			d.setClosed(Double.valueOf(fv[5]));
			d.setYesterdayPrice(Double.valueOf(fv[6]));
			d.setTodayChange(Double.valueOf(fv[7]));
			d.setTodayChangeRate(Double.valueOf(fv[8]));
			d.setVolume(Double.valueOf(fv[9]));
			try {
				d.setAmt(Double.valueOf(fv[10]));
			} catch (NumberFormatException e) {
				d.setAmt(Double.valueOf(0));
			}
			d.setId();
			return d;
		} catch (Exception e) {
			// ErrorLogFileUitl.writeError(e, "日K数据错误", "原始数据", line);
			log.info("日K数据错误,原始数据:" + line);
			return null;
//			throw new RuntimeException(e);
		}
//		return null;
	}

	/**
	 * 每日*定时任务-日交易
	 */
	public void jobSpiderAll() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.DAY) {
					public Object mycall() {
						String today = DateUtil.getTodayYYYYMMDD();
						try {
							log.info("每日*定时任务-日交易[started]");
							// 全量获取历史记录（定时任务）-根据缓存是否需要重新获取，（除权得时候会重新获取）
							// 每日更新-job
							int date = Integer.valueOf(today);
							if (tradeCalService.isOpen(date)) {
								int succ = spiderTodayDaliyTrade(today);
								if (succ > 0) {
									WxPushUtil.pushSystem1("Seq2=>正常执行=>日K复权任务,succ=" + succ);
								} else {
									WxPushUtil.pushSystem1("异常执行Seq2=>日K复权任务,succ=0");
								}
							} else {
								log.info("非工作日。");
							}
							log.info("每日*定时任务-日交易[end]");
						} finally {
							log.info("等待模型执行");
							nextSortMode6(today);
						}
						return null;
					}
				});
	}

//	private void nextModelJob(String today) {
//		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
//			@Override
//			public Object call() throws Exception {
//				try {
//					try {
//						TimeUnit.MINUTES.sleep(10);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					upLevel1Service.runJob(true, Integer.valueOf(today));
//				} finally {
//					// log.info("等待图片模型执行");
//					// nextImageJob(today);
//					log.info("等待 sort mode 执行");
//					nextSortMode6(today);
//				}
//				return null;
//			}
//		});
//	}

	public void nextSortMode6(String today) {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				try {
					TimeUnit.MINUTES.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					sortV6Service.sortv6(Integer.valueOf(today));
				} finally {
					// log.info("等待图片模型执行");
					// nextImageJob(today);
					// log.info("等待code pool 执行");
				}
				return null;
			}
		});
	}

	public void nextImageJob(String today) {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				JSONArray array;
				try {
					array = tushareSpider.getStockDaliyBasic(null, today, null, null).getJSONArray("items");
				} catch (Exception e) {
					WxPushUtil.pushSystem1("图形指标：tushare获取记录StockDaliyBasic失败,date=" + today);
					e.printStackTrace();
					return null;
				}
				if (array == null || array.size() <= 0) {
					WxPushUtil.pushSystem1("图形指标：tushare获取记录StockDaliyBasic失败,date=" + today);
				} else {
					log.info("{}获取到每日指标记录条数={}", today, array.size());
					imageCheck(array, today);
				}
				return null;
			}
		});
	}

	/**
	 * 每日*定时任务-日交易
	 */
	public void jobSpiderAll(String date) {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL) {
					public Object mycall() {
						log.info("每日*定时任务-日交易[started]");
						spiderTodayDaliyTrade(date);
						log.info("每日*定时任务-日交易[end]");
						return null;
					}
				});
	}

	/**
	 * 手动*全部历史
	 */
	public void spiderAllDirect(String date) {
		TasksWorker.getInstance().getService().submit(
				new MyCallable(RunLogBizTypeEnum.TRADE_HISTROY, RunCycleEnum.MANUAL, "手动*全部历史,日交易,date=" + date) {
					public Object mycall() {
						log.info("手动*全部历史,日交易[started]");
						List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
						for (StockBaseInfo s : list) {
							redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + s.getCode());
						}
						spiderTodayDaliyTrade(date);
						log.info("手动*全部历史,日交易[end]");
						return null;
					}
				});
	}

}
