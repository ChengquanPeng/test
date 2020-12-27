package com.stable.service.model.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsStockAvgDao;
import com.stable.es.dao.base.EsStockAvgNofqDao;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.SMAUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.StockAvgNofq;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AvgService {

	@Value("${python.file.daily.avg}")
	private String pythonFileName;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private EsStockAvgDao stockAvgDao;
	@Autowired
	private EsStockAvgNofqDao stockAvgNofqDao;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private SMAUtil smaUtil;

	private List<StockAvgBase> getSMA30(String code, int startDate, int endDate, boolean isQfq) {
		List<StockAvgBase> rs = smaUtil.getSMA5_30(code, startDate, endDate, isQfq);
		if (rs != null) {
			log.info("SMA-30 -> From 本地计算 code={},startDate={},endDate={},size={}", code, startDate, endDate,
					rs.size());
			return rs;
		} else {
			rs = getDPriceAvgFromTushar(code, startDate, endDate, isQfq);
			log.info("SMA-30 -> From Thshare");
			return rs;
		}
	}

	private List<StockAvgBase> getSMA60(String code, int startDate, int endDate, boolean isQfq) {
		List<StockAvgBase> rs = smaUtil.getSMA5_60(code, startDate, endDate, isQfq);
		if (rs != null) {
			log.info("SMA-60 -> From 本地计算 code={},startDate={},endDate={},size={}", code, startDate, endDate,
					rs.size());
			return rs;
		} else {
			rs = getDPriceAvgFromTushar(code, startDate, endDate, isQfq);
			log.info("SMA-60 -> From Thshare");
			return rs;
		}
	}

	// 比如5日均线，开始和结束日期参数跨度必须要超过5日。
	// 本方法要超过250个交易日。
	private List<StockAvgBase> getDPriceAvgFromTushar(String code, int startDate, int endDate, boolean isQfq) {
		String params = "";
		if (isQfq) {
			params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq D";
		} else {
			params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " None D";
		}

		boolean gotData = false;
		List<String> lines = null;
		int i = 0;
		do {
			// 最多返回30条, 而且日期倒序返回
			lines = PythonCallUtil.callPythonScript(pythonFileName, params);
			if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
				if (i >= 3) {
					log.warn("pythonFileName：{}，未获取到数据 params：{}", pythonFileName, code, params);
					if (lines != null && !lines.isEmpty()) {
						log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
					}
					ErrorLogFileUitl.writeError(new RuntimeException(), code, "未获取到均价信息", startDate + " " + endDate);
					return null;
				}
				ThreadsUtil.sleepRandomSecBetween5And15();
			} else {
				gotData = true;
			}
			i++;
		} while (!gotData);

		try {
			int lastDividendDate = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
			List<StockAvgBase> list30 = new LinkedList<StockAvgBase>();
			for (int j = 0; j < 30; j++) {
				String[] strs = lines.get(j).replaceAll("nan", "0").split(",");
				// code,date,3,5,10,20,30,120,250
				// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
				StockAvgBase av;
				if (isQfq) {
					av = new StockAvg();
				} else {
					av = new StockAvgNofq();
				}
				av.setCode(code);
				av.setDate(Integer.valueOf(strs[1]));
				av.setId();
//				av.setAvgPriceIndex3(Double.valueOf(strs[2]));
				av.setAvgPriceIndex5(Double.valueOf(strs[3]));
				av.setAvgPriceIndex10(Double.valueOf(strs[4]));
				av.setAvgPriceIndex20(Double.valueOf(strs[5]));
				av.setAvgPriceIndex30(Double.valueOf(strs[6]));
				av.setAvgPriceIndex60(Double.valueOf(strs[7]));
				av.setAvgPriceIndex120(Double.valueOf(strs[8]));
				av.setAvgPriceIndex250(Double.valueOf(strs[9]));
				av.setLastDividendDate(lastDividendDate);
				list30.add(av);
			}
			return list30;
		} catch (Exception e) {
			String msg = "获取到的数据:" + lines.get(0);
//			log.error(msg);
			e.printStackTrace();
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * @param code
	 * @param endDate   截止日期
	 * @param queryPage 最近的多少条，一般是30
	 * @return
	 */
	public List<StockAvgBase> queryListByCodeForModelWithLastAnd30Records(String code, int endDate, boolean isQfq) {
		return queryListByCodeForModelWithLast(code, 0, endDate, EsQueryPageUtil.queryPage30, isQfq);
	}

	public List<StockAvgBase> queryListByCodeForModelWithLast(String code, int endDate, EsQueryPageReq queryPage,
			boolean isQfq) {
		return queryListByCodeForModelWithLast(code, 0, endDate, queryPage, isQfq);
	}

	public List<StockAvgBase> queryListByCodeForModelWithLast(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, boolean isQfq) {
		int qfqDate = 0;
		if (isQfq) {
			qfqDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
		}
		List<StockAvgBase> db = queryListByCodeForModel(code, startDate, endDate, queryPage, isQfq);
		boolean needFetch = false;
		List<TradeHistInfoDaliy> tradedaliylist = null;
		List<TradeHistInfoDaliyNofq> tradedaliylistNofq = null;
		if (db != null && db.size() == queryPage.getPageSize()) {
			for (StockAvgBase r : db) {
				if (qfqDate != 0 && r.getLastDividendDate() < qfqDate) {// 存的数据是前复权日期版本小于redis，不是最新的
					needFetch = true;
					break;
				}
			}
			if (!needFetch) {
				// check 是否是正确的连续30天的数据
				if (isQfq) {
					tradedaliylist = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, startDate, endDate,
							queryPage, SortOrder.DESC);
					if (tradedaliylist != null) {
						if (tradedaliylist.get(0).getDate() == db.get(0).getDate()
								&& tradedaliylist.get(queryPage.getPageSize() - 1).getDate() == db
										.get(queryPage.getPageSize() - 1).getDate()) {
							return db;
						} else {
							needFetch = true;
						}
					} else {
						needFetch = true;
					}
				} else {
					tradedaliylistNofq = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, startDate, endDate,
							queryPage, SortOrder.DESC);
					if (tradedaliylistNofq != null) {
						if (tradedaliylistNofq.get(0).getDate() == db.get(0).getDate()
								&& tradedaliylistNofq.get(queryPage.getPageSize() - 1).getDate() == db
										.get(queryPage.getPageSize() - 1).getDate()) {
							return db;
						} else {
							needFetch = true;
						}
					} else {
						needFetch = true;
					}
				}
			}
		} else {
			// 未查询到数据或者小于30条
			needFetch = true;
		}

		if (needFetch) {
			int sd = 0;
			if (isQfq) {
				if (tradedaliylist == null) {
					// 可能在上面已经初始化。
					tradedaliylist = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, startDate, endDate,
							queryPage, SortOrder.DESC);
				}
				if (tradedaliylist != null) {
					sd = tradedaliylist.get(tradedaliylist.size() - 1).getDate();
				}
			} else {
				if (tradedaliylistNofq == null) {
					// 可能在上面已经初始化。
					tradedaliylistNofq = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, startDate, endDate,
							queryPage, SortOrder.DESC);
				}
				if (tradedaliylistNofq != null) {
					sd = tradedaliylistNofq.get(tradedaliylistNofq.size() - 1).getDate();
				}
			}
			if (sd > 0) {
				List<StockAvgBase> result = getSMA30(code, sd, endDate, isQfq);
				if (result != null && result.size() > 0) {
					if (isQfq) {
						List<StockAvg> r1 = new ArrayList<StockAvg>();
						for (StockAvgBase s : result) {
							r1.add((StockAvg) s);
						}
						stockAvgDao.saveAll(r1);
					} else {
						List<StockAvgNofq> r2 = new ArrayList<StockAvgNofq>();
						for (StockAvgBase s : result) {
							r2.add((StockAvgNofq) s);
						}
						stockAvgNofqDao.saveAll(r2);
					}

				}
				return result;
			}
		}
		return null;
	}

	public List<StockAvgBase> queryListByCodeForModelWithLast60(String code, int endDate, EsQueryPageReq queryPage,
			boolean isQfq) {
		int sd = 0;
		if (isQfq) {
			List<TradeHistInfoDaliy> tradedaliylist = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
					endDate, queryPage, SortOrder.DESC);
			if (tradedaliylist != null) {
				sd = tradedaliylist.get(tradedaliylist.size() - 1).getDate();
			}
		} else {
			List<TradeHistInfoDaliyNofq> tradedaliylistNofq = daliyTradeHistroyService.queryListByCodeWithLastNofq(code,
					0, endDate, queryPage, SortOrder.DESC);
			if (tradedaliylistNofq != null) {
				sd = tradedaliylistNofq.get(tradedaliylistNofq.size() - 1).getDate();
			}
		}
		if (sd > 0) {
			List<StockAvgBase> result = getSMA60(code, sd, endDate, isQfq);
			return result;
		}
		return null;
	}

	private List<StockAvgBase> queryListByCodeForModel(String code, int startDate, int endDate,
			EsQueryPageReq queryPage, boolean isQfq) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},trade_date={},pageNum={},size={}", code, date, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}
		bqb.must(QueryBuilders.rangeQuery("date").lte(endDate));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		if (isQfq) {
			List<StockAvg> dbs = stockAvgDao.search(sq).getContent();
			if (dbs != null && dbs.size() > 0) {
				List<StockAvgBase> rl = new ArrayList<StockAvgBase>();
				for (StockAvg s : dbs) {
					rl.add(s);
				}
				return rl;
			}
		} else {
			List<StockAvgNofq> dbs = stockAvgNofqDao.search(sq).getContent();
			if (dbs != null && dbs.size() > 0) {
				List<StockAvgBase> rl = new ArrayList<StockAvgBase>();
				for (StockAvgNofq s : dbs) {
					rl.add(s);
				}
				return rl;
			}
		}
		return null;
	}
}
