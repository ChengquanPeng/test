package com.stable.service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import com.stable.es.dao.base.DzjyDao;
import com.stable.es.dao.base.EsHolderNumDao;
import com.stable.es.dao.base.EsHolderPercentDao;
import com.stable.es.dao.base.FenHongDao;
import com.stable.es.dao.base.ForeignCapitalSumDao;
import com.stable.es.dao.base.JiejinDao;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.vo.HolderAnalyse;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.ForeignCapitalSum;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.HolderPercent;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.spi.req.EsQueryPageReq;

/**
 * 筹码
 */
@Service
//@Log4j2
public class ChipsService {
	private static final String ZF = "增发";
	@Autowired
	private EsHolderNumDao esHolderNumDao;
	@Autowired
	private EsHolderPercentDao esHolderPercentDao;
	@Autowired
	private FenHongDao fenHongDao;
	@Autowired
	private JiejinDao jiejinDao;
	@Autowired
	private ForeignCapitalSumDao foreignCapitalSumDao;
	@Autowired
	private DzjyDao dzjyDao;

	/**
	 * 外资持股（每周末更新一次)
	 */
	public ForeignCapitalSum getForeignCapitalSum(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<ForeignCapitalSum> page = foreignCapitalSumDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new ForeignCapitalSum();
	}

	/**
	 * 分红记录
	 */
	public FenHong getFenHong(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<FenHong> page = fenHongDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new FenHong();
	}

	/**
	 * 前后1年的解禁记录（2年）
	 */
	public List<Jiejin> getBf2yearJiejin(String code, int start, int end) {
		return getBf2yearJiejin(code, start, end, null);
	}

	// 顺序排序
	public List<Jiejin> getBf2yearJiejin(String code, int start, int end, String type) {
		int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
		int size = EsQueryPageUtil.queryPage9999.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (end > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").from(start).to(end));
		} else {
			bqb.must(QueryBuilders.rangeQuery("date").from(start));
		}
		if (type != null) {
			bqb.must(QueryBuilders.matchQuery("type", type));// 分词
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.ASC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<Jiejin> page = jiejinDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public int getRecentlyZfJiejin(String code) {
		Date now = new Date();
		int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -540));
		int end = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, 180));
		List<Jiejin> list = getBf2yearJiejin(code, start, end, ZF);
		if (list != null) {
			for (Jiejin jj : list) {
				if (jj.getType().contains(ZF)) {
					return jj.getDate();
				}
			}
		}
		return 0;
	}

	public List<Jiejin> getBf2yearJiejin(String code) {
		Date now = new Date();
		int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));
		int end = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, 370));
		return getBf2yearJiejin(code, start, end);
	}

	public List<Jiejin> getBf2yearJiejin(String code, int end) {
		Date now = new Date();
		int start = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));
		return getBf2yearJiejin(code, start, end);
	}

	/**
	 * -解禁记录
	 */
	public List<Jiejin> getAddJiejinList(String code, EsQueryPageReq querypage) {
		int pageNum = querypage.getPageNum();
		int size = querypage.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<Jiejin> page = jiejinDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	/**
	 * -解禁记录
	 */
	public Dzjy getLastDzjy(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<Dzjy> page = dzjyDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new Dzjy();
	}

	/**
	 * 最新的前3大股东占比
	 */
	public HolderPercent getLastHolderPercent(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<HolderPercent> page = esHolderPercentDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new HolderPercent();
	}

	/**
	 * 截止某个时间之前的最新记录
	 */
	public HolderNum getLastHolderNumBfDate(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<HolderNum> page = esHolderNumDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	public HolderNum getLastHolderNum(String code) {
		List<HolderNum> l = this.getHolderNumList45(code);
		if (l != null && l.size() > 0) {
			return l.get(0);
		}
		return null;
	}

	public List<HolderNum> getHolderNumList45(String code) {
		return getHolderNumList45(code, 0);
	}

	/**
	 * 最近记录
	 */
	public List<HolderNum> getHolderNumList45(String code, int startDate) {
		int pageNum = 0;
		int size = 0;
		if (startDate <= 0) {
			pageNum = EsQueryPageUtil.queryPage45.getPageNum();
			size = EsQueryPageUtil.queryPage45.getPageSize();
		} else {
			pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
			size = EsQueryPageUtil.queryPage9999.getPageSize();
		}

		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (startDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("date").gte(startDate));
		}

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
	 * 股东人数增长/减少分析
	 */
	public HolderAnalyse holderNumAnalyse(String code, int fourYearAgo) {
		HolderAnalyse r = new HolderAnalyse();
		try {
			List<HolderNum> list = getHolderNumList45(code, fourYearAgo);
			if (list != null && list.size() > 1) {
				HolderNum dmax = list.stream().max(Comparator.comparingInt(HolderNum::getNum)).get();
				HolderNum curr = list.get(0);
				r.setLastNum(curr.getNum());
				r.setAvgNum(curr.getAvgNumP5());
				r.setDate(curr.getDate());
				if (dmax.getDate() < curr.getDate() && dmax.getNum() > curr.getNum()) {
					double reduce = CurrencyUitl.cutProfit(dmax.getNum(), curr.getNum());
					r.setAnaRes(reduce);
				}
			}
			return r;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

//	private double holderNumAnalyse(String code, HolderAnalyse r) {
//		try {
//			List<HolderNum> list = getHolderNumList45(code);
//			if (list != null && list.size() > 1) {
//				r.setLastNum(list.get(0).getNum());
//				r.setAvgNum(list.get(0).getAvgNumP5());
//				r.setDate(list.get(0).getDate());
//				int c2 = 0;
//				int lowNum = 0;
//				// 增加
//				for (int i = 0; i < list.size() - 1; i++) {
//					if (list.get(i).getNum() >= list.get(i + 1).getNum()) {
//						c2++;
//						lowNum = list.get(i + 1).getNum();
//					} else {
//						break;
//					}
//				}
//				if (c2 > 0) {
//					int start = list.get(0).getNum();
//					// lowNum/start
//					int reducePresent = Double
//							.valueOf(CurrencyUitl.cutProfit(Double.valueOf(lowNum), Double.valueOf(start))).intValue();
//					if (c2 < 10) {
//						return Double.valueOf(reducePresent + ".0" + c2);
//					} else {
//						return Double.valueOf(reducePresent + "." + c2);
//					}
//				}
//				// 减少
//				int c1 = 0;
//				int highNum = 0;
//				for (int i = 0; i < list.size() - 1; i++) {
//					if (list.get(i).getNum() <= list.get(i + 1).getNum()) {
//						c1++;
//						highNum = list.get(i + 1).getNum();
//					} else {
//						break;
//					}
//				}
//				if (c1 > 0) {
//					int start = list.get(0).getNum();
//					// start/lowNum
//					int reducePresent = Double
//							.valueOf(CurrencyUitl.cutProfit(Double.valueOf(highNum), Double.valueOf(start))).intValue();
//					double t = 0.0;
//					if (c1 < 10) {
//						t = Double.valueOf(reducePresent + ".0" + c1);
//					} else {
//						t = Double.valueOf(reducePresent + "." + c1);
//					}
//					// 变化太小导致reducePresent=0，没有-负数符号
//					if (reducePresent == 0) {
//						return (0 - t);
//					} else {
//						return t;
//					}
//				}
//			}
//			return 0.0;
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//	}
}
