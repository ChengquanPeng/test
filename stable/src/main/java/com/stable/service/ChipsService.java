package com.stable.service;

import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsHolderNumDao;
import com.stable.es.dao.base.EsHolderPercentDao;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.HolderPercent;

/**
 * 筹码
 */
@Service
//@Log4j2
public class ChipsService {
	@Autowired
	private EsHolderNumDao esHolderNumDao;
	@Autowired
	private EsHolderPercentDao esHolderPercentDao;

	/**
	 * 最新的前2名大股东
	 */
	public double getLastHolderPercent(String code) {
		int pageNum = EsQueryPageUtil.queryPage1.getPageNum();
		int size = EsQueryPageUtil.queryPage1.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<HolderPercent> page = esHolderPercentDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0).getTopThree();
		}
		return 0.0;
	}

	/**
	 * 最近44条记录
	 */
	public List<HolderNum> getHolderNumList45(String code) {
		int pageNum = EsQueryPageUtil.queryPage45.getPageNum();
		int size = EsQueryPageUtil.queryPage45.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<HolderNum> page = esHolderNumDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	/**
	 * 股东人数增长/减少分析（幅度+次数）
	 */
	public double holderNumAnalyse(String code) {
		try {
			List<HolderNum> list = getHolderNumList45(code);
			if (list != null && list.size() > 1) {
				int c2 = 0;
				int lowNum = 0;
				// 增加
				for (int i = 0; i < list.size() - 1; i++) {
					if (list.get(i).getNum() >= list.get(i + 1).getNum()) {
						c2++;
						lowNum = list.get(i + 1).getNum();
					} else {
						break;
					}
				}
				if (c2 > 0) {
					int start = list.get(0).getNum();
					int reducePresent = Double
							.valueOf(CurrencyUitl.cutProfit(Double.valueOf(start), Double.valueOf(lowNum))).intValue();
					if (c2 < 10) {
						return Double.valueOf(reducePresent + ".0" + c2);
					} else {
						return Double.valueOf(reducePresent + "." + c2);
					}
				}
				// 减少
				int c1 = 0;
				int highNum = 0;
				for (int i = 0; i < list.size() - 1; i++) {
					if (list.get(i).getNum() <= list.get(i + 1).getNum()) {
						c1++;
						highNum = list.get(i + 1).getNum();
					} else {
						break;
					}
				}
				if (c1 > 0) {
					int start = list.get(0).getNum();
					int reducePresent = Double
							.valueOf(CurrencyUitl.cutProfit(Double.valueOf(start), Double.valueOf(highNum))).intValue();
					if (c1 < 10) {
						return Double.valueOf(reducePresent + ".0" + c1);
					} else {
						return Double.valueOf(reducePresent + "." + c1);
					}
				}
			}
			return 0.0;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
