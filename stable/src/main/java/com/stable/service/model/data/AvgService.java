package com.stable.service.model.data;

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

import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.EsStockAvgDao;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TradeHistInfoDaliy;
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
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public List<StockAvg> getWPriceAvg(String code, int startDate, int endDate) {
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq W";
		boolean gotData = false;
		List<String> lines = null;
		int i = 0;
		do {
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
			List<StockAvg> list4 = new LinkedList<StockAvg>();
			for (int j = 0; j < 4; j++) {
				String[] strs = lines.get(j).replaceAll("nan", "0").split(",");
				// code,date,3,5,10,20,30,120,250
				// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
				StockAvg av = new StockAvg();
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
				list4.add(av);
			}
			return list4;
		} catch (Exception e) {
			String msg = "获取到的数据:" + lines.get(0);
//			log.error(msg);
			throw new RuntimeException(msg, e);
		}
	}

	// 比如5日均线，开始和结束日期参数跨度必须要超过5日。
	// 本方法要超过250个交易日。
	private List<StockAvg> getDPriceAvg(String code, int startDate, int endDate) {
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq D";
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
			List<StockAvg> list30 = new LinkedList<StockAvg>();
			for (int j = 0; j < 30; j++) {
				String[] strs = lines.get(j).replaceAll("nan", "0").split(",");
				// code,date,3,5,10,20,30,120,250
				// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
				StockAvg av = new StockAvg();
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

	public StockAvg queryListByCodeForRealtime(String code) {
		return queryListByCodeForRealtime(code, 0);
	}

	public StockAvg queryListByCodeForRealtime(String code, int date) {
		try {
			BoolQueryBuilder bqb = QueryBuilders.boolQuery();
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
			if (date > 0) {
				bqb.must(QueryBuilders.matchPhraseQuery("date", date));
			}
			NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
			SearchQuery sq = queryBuilder.withQuery(bqb).build();
			return stockAvgDao.search(sq).getContent().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	EsQueryPageReq queryPage300 = new EsQueryPageReq(300);
	EsQueryPageReq queryPage30 = new EsQueryPageReq(30);

	/**
	 * @param code
	 * @param date      截止日期
	 * @param queryPage 最近的多少条，一般是30
	 * @return
	 */
	public List<StockAvg> queryListByCodeForModelWithLastQfq(String code, int date) {
		int qfqDate = Integer.valueOf(redisUtil.get(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + code, "0"));
		List<StockAvg> db = queryListByCodeForModel(code, qfqDate, queryPage30);
		boolean needFetch = false;
		if (db != null && db.size() == 30) {
			for (StockAvg r : db) {
				if (qfqDate != 0 && r.getLastDividendDate() < qfqDate) {// 存的数据是前复权日期版本小于redis，不是最新的
					needFetch = true;
					break;
				}
			}
			if (!needFetch) {
				// check 是否是正确的连续30天的数据
				List<TradeHistInfoDaliy> tradedaliylist = daliyTradeHistroyService.queryListByCode(code, 0, date,
						queryPage30, SortOrder.DESC);
				if (tradedaliylist != null) {
					if (tradedaliylist.get(0).getDate() == db.get(0).getDate()
							&& tradedaliylist.get(29).getDate() == db.get(29).getDate()) {
						return db;
					} else {
						needFetch = true;
					}
				} else {
					needFetch = true;
				}
			}
		} else {
			// 未查询到数据或者小于30条
			needFetch = true;
		}

		if (needFetch) {
			List<TradeHistInfoDaliy> tradedaliylist = daliyTradeHistroyService.queryListByCode(code, 0, date,
					queryPage300, SortOrder.ASC);
			if (tradedaliylist != null) {
				List<StockAvg> result = getDPriceAvg(code, tradedaliylist.get(0).getDate(), date);
				if (result != null && result.size() > 0) {
					stockAvgDao.saveAll(result);
				}
				return result;
			}
		}
		return null;
	}

	private List<StockAvg> queryListByCodeForModel(String code, int date, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},trade_date={},pageNum={},size={}", code, date, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return stockAvgDao.search(sq).getContent();
	}

}
