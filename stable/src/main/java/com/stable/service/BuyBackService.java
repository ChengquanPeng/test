package com.stable.service;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

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

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.RedisConstant;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsBuyBackInfoDao;
import com.stable.job.MyCallable;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.http.resp.BuyBackInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 回购
 * 
 * @author roy
 *
 */
@Service
@Log4j2
public class BuyBackService {
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RedisUtil redisUtil;

	public void spiderBuyBackHistoryInfo() {
		this.spiderBuyBackHistoryInfo(null, null);
	}

	public void spiderBuyBackHistoryInfo(String start_date, String end_date) {
		TasksWorker.getInstance().getService().submit(new Callable<Object>() {
			public Object call() throws Exception {
				fetchHist(start_date, end_date);
				return null;
			}
		});
	}

	public void jobFetchHistEveryDay() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.BUY_BACK, RunCycleEnum.DAY) {
			public Object mycall() {
				String rv = redisUtil.get(RedisConstant.RDS_BUY_BACK_LAST_DAY);
				Date lastDate = null;
				Date todayDate = new Date();
				if (StringUtils.isBlank(rv)) {
					lastDate = DateUtil.addDate(todayDate, -1);
				} else {
					lastDate = DateUtil.parseDate(rv);
				}
				int today = Integer.valueOf(DateUtil.getYYYYMMDD(todayDate));
				do {
					lastDate = DateUtil.addDate(lastDate, 1);// 加一天
					int last = Integer.valueOf(DateUtil.getYYYYMMDD(lastDate));
					if (last > today) {
						break;
					}
					String ann_date = String.valueOf(last);
					log.info("同步回购公告列表[started],ann_date={},", ann_date);
					JSONArray array = tushareSpider.getBuyBackList(null, null, ann_date);
					// System.err.println(array.toJSONString());
					if (array != null) {
						log.info("获取到回购公告记录条数={}", array.size());
						for (int i = 0; i < array.size(); i++) {
							BuyBackInfo base = new BuyBackInfo(array.getJSONArray(i));
							// if(i==0) {
							buyBackInfoDao.save(base);
							// }
							// System.err.println(base);
						}
						redisUtil.set(RedisConstant.RDS_BUY_BACK_LAST_DAY, last);
					} else {
						log.info("未获取到回购公告");
					}
					log.info("同步回购公告列表[end],ann_date={}", ann_date);

				} while (true);
				return null;
			}
		});
	}

	public void jobFetchHist() {
		TasksWorker.getInstance().getService().submit(new MyCallable(RunLogBizTypeEnum.BUY_BACK, RunCycleEnum.WEEK) {
			public Object mycall() {
				Calendar cal = Calendar.getInstance();
				String startDate = "", endDate = "";
				int first = 0, last = 0;
				first = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
				cal.set(Calendar.DAY_OF_MONTH, first);
				startDate = DateUtil.getYYYYMMDD(cal.getTime());
				last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
				cal.set(Calendar.DAY_OF_MONTH, last);
				endDate = DateUtil.getYYYYMMDD(cal.getTime());
				fetchHist(startDate, endDate);
				return null;
			}
		});
	}

	public void fetchHist(String start_date, String end_date) {
		log.info("同步回购公告列表[started],start_date={},end_date={},", start_date, end_date);
		JSONArray array = tushareSpider.getBuyBackList(start_date, end_date, null);
		// System.err.println(array.toJSONString());
		for (int i = 0; i < array.size(); i++) {
			BuyBackInfo base = new BuyBackInfo(array.getJSONArray(i));
			// if(i==0) {
			buyBackInfoDao.save(base);
			// }
			// System.err.println(base);
		}
		log.info("同步回购公告列表[end],start_date={},end_date={},", start_date, end_date);
	}

	public List<BuyBackInfoResp> getListByCodeForWebPage(String code, int dtype, int asc, EsQueryPageReq querypage) {
		List<BuyBackInfoResp> res = new LinkedList<BuyBackInfoResp>();
		List<BuyBackInfo> list = this.getBuyBackInfo(code, dtype, asc, querypage);
		if (list != null) {
			for (BuyBackInfo dh : list) {
				BuyBackInfoResp resp = new BuyBackInfoResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<BuyBackInfo> getBuyBackInfo(String code, int dtype, int asc, EsQueryPageReq querypage) {
		log.info("query code={},dtype={},asc={}", code, dtype, asc);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		// 提议/预案/股东大会通过/实施/完成/停止
		if (dtype == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "提议"));
		} else if (dtype == 2) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "预案"));
		} else if (dtype == 3) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "股东大会通过"));
		} else if (dtype == 4) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "实施"));
		} else if (dtype == 5) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "完成"));
		} else if (dtype == 6) {
			bqb.must(QueryBuilders.matchPhraseQuery("proc", "停止"));
		}
		// 全部
		SortOrder s = SortOrder.DESC;
		if (asc == 1) {
			s = SortOrder.ASC;
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("ann_date").unmappedType("integer").order(s);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder = queryBuilder.withQuery(bqb);
		if (pageable != null) {
			queryBuilder = queryBuilder.withPageable(pageable);
		}
		queryBuilder = queryBuilder.withSort(sort);
		SearchQuery sq = queryBuilder.build();

		Page<BuyBackInfo> page = buyBackInfoDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}
}
