package com.stable.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.stable.es.dao.base.EsCodePoolDao;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.CodePool;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 股票池
 */
@Service
@Log4j2
public class CodePoolService {

	@Autowired
	private EsCodePoolDao codePoolDao;

	public void addMid(String code, int inout) {
		CodePool c = getCodePool(code);
		c.setMidOk(inout);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		if (inout == 1) {
			c.setMidRemark(c.getUpdateDate() + " 系统中线");
		} else {
			c.setMidRemark("");
		}

		codePoolDao.save(c);
	}

	public void addSortV4(String code, int inout) {
		CodePool c = getCodePool(code);
		c.setSortOk(inout);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		if (inout == 1) {
			c.setSortV4Remark(c.getUpdateDate() + " sort v4 短线");
		} else {
			c.setSortV4Remark("");
		}
		codePoolDao.save(c);
	}

	public void addManual(String code, int inout, String remark) {
		CodePool c = getCodePool(code);
		c.setManualOk(inout);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		if (inout == 1) {
			c.setRemark(c.getUpdateDate() + " " + remark);
		} else {
			c.setRemark("");
		}
		codePoolDao.save(c);
	}

	public CodePool getCodePool(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		SearchQuery sq = queryBuilder.build();
		Page<CodePool> page = codePoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		CodePool cp = new CodePool();
		cp.setCode(code);
		return cp;
	}

	public List<CodePool> getCodePool() {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		if (pageable != null) {
			queryBuilder = queryBuilder.withPageable(pageable);
		}
		SearchQuery sq = queryBuilder.build();

		Page<CodePool> page = codePoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public Map<String, CodePool> getCodePoolMap() {
		Map<String, CodePool> map = new HashMap<String, CodePool>();
		List<CodePool> list = this.getCodePool();
		if (list != null) {
			for (CodePool c : list) {
				map.put(c.getCode(), c);
			}
		}
		return map;
	}

	public void saveAll(List<CodePool> list) {
		if (list != null && list.size() > 0) {
			codePoolDao.saveAll(list);
		}
	}

	/**
	 * 监听列表
	 */
	private List<CodePool> getPoolList(String code, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else {
			// 监听列表
			bqb.must(QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("midOk", 1))
					.should(QueryBuilders.matchQuery("sortOk", 1)).should(QueryBuilders.matchQuery("manualOk", 1)));
		}

		FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<CodePool> page = codePoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	/**
	 * 基本面列表
	 */
	private List<CodePool> getPoolList(String code, int inMid, int midOk, int sortOk, int manualOk,
			EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (inMid >= 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("inMid", inMid));
		}
		if (midOk >= 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("midOk", midOk));
		}
		if (sortOk >= 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("sortOk", sortOk));
		}
		if (manualOk >= 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("manualOk", manualOk));
		}

		FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<CodePool> page = codePoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<CodePool> getListByCodeForWebPage(String code, String year, String quarter, EsQueryPageReq queryPage) {
//		List<FinanceBaseInfoResp> res = new LinkedList<FinanceBaseInfoResp>();
//		List<FinanceBaseInfo> list = this.getFinaceReports(code, year, quarter, queryPage);
//		if (list != null) {
//			for (FinanceBaseInfo dh : list) {
//				FinanceBaseInfoResp resp = new FinanceBaseInfoResp();
//				resp.setCode(dh.getCode());
//				resp.setEndDate(String.valueOf(dh.getDate()));
//				resp.setEndType(dh.getYear(), dh.getQuarter());
//				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
//				resp.setYyzsr(CurrencyUitl.covertToString(dh.getYyzsr()));
//				resp.setGsjlr(CurrencyUitl.covertToString(dh.getGsjlr()));
//				resp.setKfjlr(CurrencyUitl.covertToString(dh.getKfjlr()));
//				resp.setYyzsrtbzz(dh.getYyzsrtbzz());
//				resp.setGsjlrtbzz(dh.getGsjlrtbzz());
//				resp.setKfjlrtbzz(dh.getKfjlrtbzz());
//				res.add(resp);
//			}
//		}
		return null;
	}
}
