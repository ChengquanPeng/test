package com.stable.service;

import java.math.BigDecimal;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsReducingHoldingSharesDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.ReducingHoldingSharesStat;
import com.stable.vo.bus.ReducingHoldingShares;

/**
 * 减持
 */
@Service
public class ReducingHoldingSharesService {
	@Autowired
	private EsReducingHoldingSharesDao rhssDao;
	@Autowired
	private StockBasicService stockBasicService;

	public ReducingHoldingSharesStat getLast(String code, int pre1year) {
		ReducingHoldingSharesStat r = new ReducingHoldingSharesStat();
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").gte(pre1year));
		int pageNum = EsQueryPageUtil.queryPage500.getPageNum();
		int size = EsQueryPageUtil.queryPage500.getPageSize();
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(PageRequest.of(pageNum, size)).build();

		List<ReducingHoldingShares> page = rhssDao.search(sq).getContent();
		if (page != null && !page.isEmpty()) {
			double gw = 0;
			for (ReducingHoldingShares row : page) {
				gw += row.getWg();

				if (row.getType() == 2 && row.getDate() > r.getLastPlanDate()) {
					r.setLastPlanDate(row.getDate());
				}
			}
			r.setTims(page.size());
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
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(PageRequest.of(pageNum, size)).build();
		return rhssDao.search(sq).getContent();
	}
}
