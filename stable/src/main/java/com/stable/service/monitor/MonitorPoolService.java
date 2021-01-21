package com.stable.service.monitor;

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
import com.stable.enums.CodeModeType;
import com.stable.es.dao.base.EsCodeBaseModelDao;
import com.stable.es.dao.base.MonitorPoolDao;
import com.stable.service.ConceptService;
import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.MonitorPool;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.http.resp.MonitorPoolResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 监听池
 */
@Service
@Log4j2
public class MonitorPoolService {
	@Autowired
	private MonitorPoolDao monitorPoolDao;
	@Autowired
	private EsCodeBaseModelDao codeBaseModelDao;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ConceptService conceptService;

	// 移除监听
	public void delMonit(String code, String remark) {
		MonitorPool c = getMonitorPool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(0);
		c.setUpPrice(0);
		c.setDownPrice(0);
		c.setUpTodayChange(0);
		c.setDownTodayChange(0);
		c.setRealtime(0);
		c.setOffline(0);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + c.getUpdateDate());
		}
		monitorPoolDao.save(c);
		updateBaseMoniStatus(code, c.getMonitor());
	}

	private void updateBaseMoniStatus(String code, int monitor) {
		CodeBaseModel cbm = codeModelService.getLastOneByCode(code);
		cbm.setMonitor(monitor);
		codeBaseModelDao.save(cbm);
	}

	// 加入监听
	public void addMonitor(String code, int monitor, int realtime, int offline, double upPrice, double downPrice,
			double upTodayChange, double downTodayChange, String remark) {
		if (monitor <= 0) {
			throw new RuntimeException("monitor<=0 ?");
		}
		if (realtime == 0 && offline == 0) {
			throw new RuntimeException("realtime == 0 && offline == 0 ?");
		}
		if (upPrice == 0 && upTodayChange == 0 && downPrice == 0 && downTodayChange == 0) {
			throw new RuntimeException(
					"upPrice == 0 && upTodayChange == 0 && downPrice == 0 && downTodayChange == 0 ?");
		}
		MonitorPool c = getMonitorPool(code);
		c.setUpdateDate(DateUtil.getTodayIntYYYYMMDD());
		c.setMonitor(monitor);
		c.setRealtime(realtime);
		c.setOffline(offline);
		c.setDownPrice(downPrice);
		c.setDownTodayChange(downTodayChange);
		c.setUpPrice(upPrice);
		c.setUpTodayChange(upTodayChange);
		if (StringUtils.isBlank(remark)) {
			c.setRemark("");
		} else {
			c.setRemark(remark + " " + c.getUpdateDate());
		}
		monitorPoolDao.save(c);
		updateBaseMoniStatus(code, c.getMonitor());
	}

	public MonitorPool getMonitorPool(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		SearchQuery sq = queryBuilder.build();
		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		MonitorPool cp = new MonitorPool();
		cp.setCode(code);
		return cp;
	}

	// 所有监听池
	public List<MonitorPool> getMonitorPool() {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		if (pageable != null) {
			queryBuilder = queryBuilder.withPageable(pageable);
		}
		SearchQuery sq = queryBuilder.build();

		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public Map<String, MonitorPool> getMonitorPoolMap() {
		return getPoolMap(this.getMonitorPool());
	}

	public Map<String, MonitorPool> getPoolMap(List<MonitorPool> list) {
		Map<String, MonitorPool> map = new HashMap<String, MonitorPool>();
		if (list != null) {
			for (MonitorPool c : list) {
				map.put(c.getCode(), c);
			}
		}
		return map;
	}

	public Map<String, TradeHistInfoDaliyNofq> getPoolMap2(List<TradeHistInfoDaliyNofq> list) {
		Map<String, TradeHistInfoDaliyNofq> map = new HashMap<String, TradeHistInfoDaliyNofq>();
		if (list != null) {
			for (TradeHistInfoDaliyNofq c : list) {
				map.put(c.getCode(), c);
			}
		}
		return map;
	}

	/**
	 * 监听列表-实时
	 */
	public List<MonitorPool> getPoolListForMonitor(int realtime, int offline) {
		int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
		int size = EsQueryPageUtil.queryPage9999.getPageSize();
		log.info("queryPage pageNum={},size={}", pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		// 监听列表 should OR 或 查询
		bqb.must(QueryBuilders.rangeQuery("monitor").gt(0));
		if (realtime > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("realtime", 1));
		}
		if (offline > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("offline", 1));
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<MonitorPoolResp> getListForWeb(String code, int monitor, int monitoreq, EsQueryPageReq querypage,
			String aliasCode) {
		log.info("CodeBaseModel getListForWeb code={},num={},size={},aliasCode={},monitor={},monitoreq={}", code,
				querypage.getPageNum(), querypage.getPageSize(), aliasCode, monitor, monitoreq);

		List<MonitorPool> list = getList(code, monitor, monitoreq, querypage, aliasCode);
		List<MonitorPoolResp> res = new LinkedList<MonitorPoolResp>();
		if (list != null) {
			for (MonitorPool dh : list) {
				MonitorPoolResp resp = new MonitorPoolResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setMonitorDesc(CodeModeType.getCodeName(dh.getMonitor()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<MonitorPool> getList(String code, int monitor, int monitoreq, EsQueryPageReq querypage,
			String aliasCode) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		} else if (StringUtils.isNotBlank(aliasCode)) {
			List<String> list = this.conceptService.listCodesByAliasCode(aliasCode);
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		}
		if (monitor > 0) {
			bqb.must(QueryBuilders.rangeQuery("monitor").gt(0));
		}
		if (monitoreq > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("monitor", monitoreq));
		}

		FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<MonitorPool> page = monitorPoolDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}
}
