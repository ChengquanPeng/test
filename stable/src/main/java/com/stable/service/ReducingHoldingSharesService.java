package com.stable.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsReducingHoldingSharesDao;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.vo.ReducingHoldingSharesStat;
import com.stable.vo.bus.ReducingHoldingShares;
import com.stable.vo.bus.StockBaseInfo;

/**
 * 减持
 */
@Service
public class ReducingHoldingSharesService {
	@Autowired
	private EsReducingHoldingSharesDao dao;
	@Autowired
	private StockBasicService stockBasicService;

	private Map<String, ReducingHoldingSharesStat> datacash = new ConcurrentHashMap<String, ReducingHoldingSharesStat>();

	public ReducingHoldingSharesStat getLastStat(String code, int pre1year) {
		ReducingHoldingSharesStat r = datacash.get(code);
		if (r == null) {
			if (pre1year <= 0) {
				pre1year = DateUtil.getPreYear(DateUtil.getTodayIntYYYYMMDD());
			}
			datacash.put(code, getLast(code, pre1year));
		}
		return r;
	}

	public void init() {
		int pre1year = DateUtil.getPreYear(DateUtil.getTodayIntYYYYMMDD());
		ConcurrentHashMap<String, ReducingHoldingSharesStat> tmp = new ConcurrentHashMap<String, ReducingHoldingSharesStat>();

		List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithOutSort();
		for (StockBaseInfo s : list) {
			tmp.put(s.getCode(), getLastStat(s.getCode(), pre1year));
		}
		datacash = tmp;
	}

	private ReducingHoldingSharesStat getLast(String code, int pre1year) {
		ReducingHoldingSharesStat r = new ReducingHoldingSharesStat();
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(pre1year));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		List<ReducingHoldingShares> page = dao.search(sq).getContent();
		if (page != null && !page.isEmpty()) {
			double gw = 0;
			for (ReducingHoldingShares row : page) {
				gw += row.getWg();
			}
			r.setT(page.size());
			double floatShare = stockBasicService.getCode(code).getFloatShare();// 亿股
			if (gw > 0) {
				BigDecimal yg = CurrencyUitl.divideDecimal(gw, CurrencyUitl.WAN_N.doubleValue());
				r.setYg(yg.doubleValue());
				if (floatShare > 0) {
					r.setZb(CurrencyUitl.roundHalfUp(((r.getYg() / floatShare) * 100)));
				}
			}
		}
		return r;
	}

	public List<ReducingHoldingShares> getLastStat(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		int pageNum = EsQueryPageUtil.queryPage500.getPageNum();
		int size = EsQueryPageUtil.queryPage500.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return dao.search(sq).getContent();
	}
}
