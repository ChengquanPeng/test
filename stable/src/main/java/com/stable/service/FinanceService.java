package com.stable.service;

import java.util.LinkedList;
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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.EsFinanceBaseInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.http.resp.FinanceBaseInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 财务
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class FinanceService {

	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsFinanceBaseInfoDao esFinanceBaseInfoDao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RedisUtil redisUtil;

	/**
	 * 删除redis，从头开始获取
	 */
	public boolean spiderFinaceHistoryInfoFromStart(String code) {
		redisUtil.set(RedisConstant.RDS_FINACE_HIST_INFO_ + code, 0);
		return spiderFinaceHistoryInfo(code);
	}

	private boolean spiderFinaceHistoryInfo(String code) {
		JSONObject datas = tushareSpider.getIncome(TushareSpider.formatCode(code));
		if (datas == null || datas.getJSONArray("items").size() <= 0) {
			log.warn("未抓取到Finane记录,code={}", code);
			return false;
		}
		String yyyy = redisUtil.get(RedisConstant.RDS_FINACE_HIST_INFO_ + code);
		int year = (StringUtils.isBlank(yyyy) ? 0 : Integer.valueOf(yyyy));
		JSONArray fields = datas.getJSONArray("fields");
		JSONArray items = datas.getJSONArray("items");
		FinanceBaseInfo f = null;
		for (int i = items.size(); i > 0; i--) {
			f = new FinanceBaseInfo();
			f.setValue(code, fields, items.getJSONArray(i - 1));
			if (f.getYear() >= year) {
				esFinanceBaseInfoDao.save(f);
				log.info("Finace income saved code={},getAnn_date={}", code, f.getAnn_date());

			}
		}
		if (f != null) {
			redisUtil.set(RedisConstant.RDS_FINACE_HIST_INFO_ + code, f.getYear());
		}
		return true;
	}

	public List<FinanceBaseInfo> getFinaceReports(String code, String year, String quarter, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},pageNum={},size={}", code, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(year)) {
			bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		}
		if (StringUtils.isNotBlank(quarter)) {
			bqb.must(QueryBuilders.matchPhraseQuery("quarter", quarter));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("end_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public List<FinanceBaseInfoResp> getListByCodeForWebPage(String code, String year, String quarter,
			EsQueryPageReq queryPage) {
		List<FinanceBaseInfoResp> res = new LinkedList<FinanceBaseInfoResp>();
		List<FinanceBaseInfo> list = this.getFinaceReports(code, year, quarter, queryPage);
		if (list != null) {
			for (FinanceBaseInfo dh : list) {
				FinanceBaseInfoResp resp = new FinanceBaseInfoResp();
				resp.setCode(dh.getCode());
				resp.setAnnDate(String.valueOf(dh.getF_ann_date()));
				resp.setEndDate(dh.getYear(), dh.getQuarter());
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setTotalRevenue(CurrencyUitl.covertToString(dh.getTotal_revenue()));
				resp.setTotalCogs(CurrencyUitl.covertToString(dh.getTotal_cogs()));
				resp.setTotalProfit(CurrencyUitl.covertToString(dh.getTotal_profit()));
				resp.setOperateProfit(CurrencyUitl.covertToString(dh.getOperate_profit()));
				resp.setIncomeTax(CurrencyUitl.covertToString(dh.getIncome_tax()));
				resp.setIncome(CurrencyUitl.covertToString(dh.getN_income()));
				resp.setFinExp(CurrencyUitl.covertToString(dh.getFin_exp()));
				resp.setBasicEps(String.valueOf(dh.getBasic_eps()));
				res.add(resp);
			}
		}
		return res;
	}

	public FinanceBaseInfo getLastFinaceReport(String code) {
		Pageable pageable = PageRequest.of(0, 1);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("end_date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

		Page<FinanceBaseInfo> page = esFinanceBaseInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			FinanceBaseInfo f = page.getContent().get(0);
			log.info("page size={},last report fince code={},date={}", page.getContent().size(), code, f.getEnd_date());
			return f;
		}
		log.info("no last report fince code={}", code);
		return null;
	}

	public void jobSpiderFinaceHistoryInfo() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.FINACE_HISTORY, RunCycleEnum.WEEK) {
					public Object mycall() {
						log.info("同步股票报告[started]");
						List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
						log.info("股票总数：" + list.size());
						for (StockBaseInfo s : list) {
							spiderFinaceHistoryInfo(s.getCode());
						}
						log.info("同步股票报告[end]");
						return null;
					}
				});
	}
}
