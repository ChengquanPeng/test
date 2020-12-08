package com.stable.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.es.dao.base.EsCodePoolDao;
import com.stable.vo.bus.CodePool;
import com.stable.vo.spi.req.EsQueryPageReq;

/**
 * 股票池
 */
@Service
public class CodePoolService {

	@Autowired
	private EsCodePoolDao codePoolDao;
//	@Autowired
//	private RedisUtil redisUtil;

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
}
