package com.stable.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsCodePoolDao;
import com.stable.service.model.CodeModelService;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.CodePool;
import com.stable.vo.http.resp.CodePoolResp;
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
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;

	public void delMonit(String code, String remark) {
		CodePool c = getCodePool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(0);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + c.getUpdateDate());
		}
		codePoolDao.save(c);
	}

	public void addMid(String code, String remark) {
		CodePool c = getCodePool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(2);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + c.getUpdateDate());
		}
		codePoolDao.save(c);
	}

	public void addManual(String code, String remark) {
		CodePool c = getCodePool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(3);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + c.getUpdateDate());
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
	public List<CodePool> getPoolList() {
		int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
		int size = EsQueryPageUtil.queryPage9999.getPageSize();
		log.info("queryPage pageNum={},size={}", pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		// 监听列表 should OR 或 查询
		bqb.must(QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("midOk", 1))
				.should(QueryBuilders.matchQuery("sortOk", 1)).should(QueryBuilders.matchQuery("manualOk", 1)));

		FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<CodePool> page = codePoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<CodePoolResp> getListForWeb(String code, String aliasCode, int asc, int monitor, int monitoreq,
			int suspectBigBoss, int inmid, double pe, double pettm, double pb, EsQueryPageReq querypage, int jiduc) {
		log.info(
				"CodeBaseModel getListForWeb code={},asc={},num={},size={},aliasCode={},monitor={},monitoreq={},pe={},pettm={},pb={}",
				code, asc, querypage.getPageNum(), querypage.getPageSize(), aliasCode, monitor, monitoreq, pe, pettm,
				pb);

		List<CodePool> list = getList(code, aliasCode, asc, monitor, monitoreq, suspectBigBoss, inmid, pe, pettm, pb,
				querypage, jiduc);
		List<CodePoolResp> res = new LinkedList<CodePoolResp>();
		if (list != null) {
			for (CodePool dh : list) {
				CodePoolResp resp = new CodePoolResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setYjlx(dh.getContinYj1() + "/" + dh.getContinYj2());
				resp.setMonitorDesc(getDesc(dh.getMonitor()));
				res.add(resp);
			}
		}
		return res;
	}

	private String getDesc(int mo) {
		switch (mo) {
		case 1:
			return "疑似大牛";
		case 2:
			return "中线";
		case 3:
			return "人工";
		case 4:
			return "短线";
		default:
			return "无";
		}
	}

	public List<CodePool> getList(String code, String aliasCode, int asc, int monitor, int monitoreq,
			int suspectBigBoss, int inmid, double pe, double pettm, double pb, EsQueryPageReq querypage, int jiduc) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else if (StringUtils.isNotBlank(aliasCode)) {
			List<String> list = this.codeModelService.listCodeByCodeConceptId(aliasCode);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		}
		if (jiduc > 0) {
			bqb.must(QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("continYj1", jiduc))
					.should(QueryBuilders.matchQuery("continYj2", jiduc)));
		}
		if (monitor > 0) {
			bqb.must(QueryBuilders.rangeQuery("monitor").gt(0));
		}
		if (monitoreq > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("monitor", monitoreq));
		}
		if (suspectBigBoss > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("suspectBigBoss", 1));
		}
		if (inmid > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("inmid", 1));
		}
		if (pe > 0) {
			bqb.must(QueryBuilders.rangeQuery("pe").gt(0).lte(pe));
		}
		if (pettm > 0) {
			bqb.must(QueryBuilders.rangeQuery("pe_ttm").gt(0).lte(pettm));
		}
		if (pb > 0) {
			bqb.must(QueryBuilders.rangeQuery("pb").gt(0).lte(pb));
		}
		SortOrder order = SortOrder.DESC;
		if (asc == 2) {
			order = SortOrder.ASC;
		}

		FieldSortBuilder sort = SortBuilders.fieldSort("pe_ttm").unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodePool> page = codePoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}
}
