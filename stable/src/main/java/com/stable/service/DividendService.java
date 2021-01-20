package com.stable.service;

import java.util.LinkedList;
import java.util.List;

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

import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.BonusHistDao;
import com.stable.job.MyCallable;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.BonusHist;
import com.stable.vo.http.resp.DividendHistoryResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 描述：分红送股数据
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class DividendService {
	public final String GDDH = "股东大会通过";
	public final String SS = "实施方案";
	@Autowired
	private BonusHistDao bonusHistDao;
	@Autowired
	private StockBasicService stockBasicService;

	@Autowired
	private DaliyTradeHistroyService daliydTradeHistroyService;
	@Autowired
	private RedisUtil redisUtil;

	public List<BonusHist> getListByCode(String code, String proc, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (StringUtils.isNotBlank(proc)) {
			bqb.must(QueryBuilders.matchPhraseQuery("status", proc));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("rptDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<BonusHist> page = bonusHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no DividendHistory for code={}", code);
		return null;
	}

	public List<DividendHistoryResp> getListByCodeForWebPage(String code, String proc, EsQueryPageReq querypage) {
		List<DividendHistoryResp> res = new LinkedList<DividendHistoryResp>();
		List<BonusHist> list = this.getListByCode(code, proc, querypage);
		if (list != null) {
			for (BonusHist dh : list) {
				DividendHistoryResp resp = new DividendHistoryResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public BonusHist getLastRecordByLteDate(String code, int start, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("dividendDate").gte(start).lte(date));
		bqb.must(QueryBuilders.termsQuery("status", GDDH, SS));// 股东大会通过
		FieldSortBuilder sort = SortBuilders.fieldSort("dividendDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();

		Page<BonusHist> page = bonusHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		log.info("no DividendHistory code={},start={},date={},", code, start, date);
		return null;
	}

	public List<BonusHist> get7DayRangeList(String start, String end) {
		Pageable pageable = PageRequest.of(0, 10000);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		// 7天左右需要除权的
		bqb.must(QueryBuilders.rangeQuery("dividendDate").gte(Integer.valueOf(start)));
		bqb.must(QueryBuilders.rangeQuery("dividendDate").lte(Integer.valueOf(end)));
		FieldSortBuilder sort = SortBuilders.fieldSort("dividendDate").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();
		Page<BonusHist> page = bonusHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	/**
	 * 前复权除权重新获取
	 */
	public void jobRespiderDaliyRecords() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.DIVIDEND_TRADE_HISTROY, RunCycleEnum.DAY) {
					@Override
					public Object mycall() {
						String start = DateUtil.getTodayBefor7DayYYYYMMDD();
						String end = DateUtil.getTodayYYYYMMDD();
						List<BonusHist> list = get7DayRangeList(start, end);
						if (list != null) {
//							StringBuffer sb = new StringBuffer();
							for (BonusHist d : list) {
								log.info("今日分红除权相关信息{}", d);
								daliydTradeHistroyService.removeCacheByChuQuan(d.getCode());
								redisUtil.set(RedisConstant.RDS_DIVIDEND_LAST_DAY_ + d.getCode(),
										String.valueOf(d.getDividendDate()));
//								sb.append(d.getCode()).append(",");
							}
							if (list.size() > 0) {
								WxPushUtil.pushSystem1(start + "-" + end + " 实施[" + list.size() + "]条分红分股！");
							} else {
								WxPushUtil.pushSystem1(start + "-" + end + "无实施分红分股");
							}
						} else {
							log.info("今日无股票分红除权相关信息");
							WxPushUtil.pushSystem1("今日无实施分红分股");
						}
						return null;
					}
				});
	}

}
