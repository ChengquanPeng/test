package com.stable.service;

import java.util.Date;

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
import com.stable.es.dao.base.DzjyDao;
import com.stable.es.dao.base.EsBuyBackInfoDao;
import com.stable.es.dao.base.EsCodeConceptDao;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.es.dao.base.EsReducingHoldingSharesDao;
import com.stable.es.dao.base.RzrqDaliyDao;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.BuyBackInfo;
import com.stable.vo.bus.CodeConcept;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.Dzjy;
import com.stable.vo.bus.ReducingHoldingShares;
import com.stable.vo.bus.RzrqDaliy;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class DeleteDataService {
	@Autowired
	private EsConceptDao esConceptDao;
	@Autowired
	private EsCodeConceptDao esCodeConceptDao;
	@Autowired
	private RzrqDaliyDao rzrqDaliyDao;
	@Autowired
	private EsBuyBackInfoDao buyBackInfoDao;
	@Autowired
	private EsReducingHoldingSharesDao reducingHoldingSharesDao;
	@Autowired
	private DzjyDao dzjyDao;
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;

//	@javax.annotation.PostConstruct
//	public void testAll() throws Exception {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				deleteData();
//				log.info("done");
//			}
//
//		}).start();
//	}

	public void deleteData() {
		// 概念未更新
		deleteInvaildCodeConcept();
		// 融资融券daily
		deleteRzrqDaliy();
		// 增减持
		deleteReducingHoldingShares();
		// 增持回购
		deleteBuyBackInfo();
		// 大宗交易
		deleteDzjy();
		// 每日指标
		deleteDaliyBasicInfo2();
	}

	// 每日指标
	private void deleteDaliyBasicInfo2() {
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
				EsQueryPageUtil.queryPage9999.getPageSize());
		int lastupdateTime = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -720));
		log.info("DaliyBasicInfo2 invaild lte=" + lastupdateTime);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("date").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		while (true) {
			Page<DaliyBasicInfo2> page = esDaliyBasicInfoDao.search(sq);
			if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
				log.info("delete  DaliyBasicInfo2 size=" + page.getContent().size());
				esDaliyBasicInfoDao.deleteAll(page.getContent());
			} else {
				log.info("delete  DaliyBasicInfo2 size=0");
				break;
			}
		}
	}

	// 大宗交易
	private void deleteDzjy() {
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
				EsQueryPageUtil.queryPage9999.getPageSize());
		int lastupdateTime = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -720));
		log.info("Dzjy invaild lte=" + lastupdateTime);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("date").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		while (true) {
			Page<Dzjy> page = dzjyDao.search(sq);
			if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
				log.info("delete  Dzjy size=" + page.getContent().size());
				dzjyDao.deleteAll(page.getContent());
			} else {
				log.info("delete  Dzjy size=0");
				break;
			}
		}
	}

	// 增持回购
	private void deleteBuyBackInfo() {
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
				EsQueryPageUtil.queryPage9999.getPageSize());
		int lastupdateTime = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1000));
		log.info("BuyBackInfo invaild lte=" + lastupdateTime);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("date").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		while (true) {
			Page<BuyBackInfo> page = buyBackInfoDao.search(sq);
			if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
				log.info("delete  BuyBackInfo size=" + page.getContent().size());
				buyBackInfoDao.deleteAll(page.getContent());
			} else {
				log.info("delete  BuyBackInfo size=0");
				break;
			}
		}
	}

	// 增减持
	private void deleteReducingHoldingShares() {
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
				EsQueryPageUtil.queryPage9999.getPageSize());
		int lastupdateTime = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1000));
		log.info("ReducingHoldingShares invaild lte=" + lastupdateTime);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("date").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		while (true) {
			Page<ReducingHoldingShares> page = reducingHoldingSharesDao.search(sq);
			if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
				log.info("delete  ReducingHoldingShares size=" + page.getContent().size());
				reducingHoldingSharesDao.deleteAll(page.getContent());
			} else {
				log.info("delete  ReducingHoldingShares size=0");
				break;
			}
		}
	}

	// 融资融券daily
	private void deleteRzrqDaliy() {
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
				EsQueryPageUtil.queryPage9999.getPageSize());
		int lastupdateTime = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -720));
		log.info("RzrqDaliy invaild lte=" + lastupdateTime);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("date").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		while (true) {
			Page<RzrqDaliy> page = rzrqDaliyDao.search(sq);
			if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
				log.info("delete  RzrqDaliy size=" + page.getContent().size());
				rzrqDaliyDao.deleteAll(page.getContent());
			} else {
				log.info("delete  RzrqDaliy size=0");
				break;
			}
		}
	}

	// 超过30天未更新,则删除
	private void deleteInvaildCodeConcept() {
		Pageable pageable = PageRequest.of(EsQueryPageUtil.queryPage9999.getPageNum(),
				EsQueryPageUtil.queryPage9999.getPageSize());
		int lastupdateTime = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -30));
		log.info("deleteAll invaild lte=" + lastupdateTime);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.rangeQuery("updateTime").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<CodeConcept> page = esCodeConceptDao.search(sq);
		if (page != null && !page.isEmpty() && page.getContent().size() > 0) {
			log.info("deleteAll CodeConcept size=" + page.getContent().size());
			esCodeConceptDao.deleteAll(page.getContent());
		} else {
			log.info("deleteAll CodeConcept size=0");
		}
		log.info("deleteAll done CodeConcept ");

		BoolQueryBuilder bqb2 = QueryBuilders.boolQuery();
		bqb2.must(QueryBuilders.rangeQuery("updateDate").lte(lastupdateTime));
		NativeSearchQueryBuilder queryBuilder2 = new NativeSearchQueryBuilder();
		SearchQuery sq2 = queryBuilder2.withQuery(bqb2).withPageable(pageable).build();

		Page<Concept> page2 = esConceptDao.search(sq2);
		if (page2 != null && !page2.isEmpty() && page2.getContent().size() > 0) {
			log.info("2Concept deleteAll size=" + page2.getContent().size());
			esConceptDao.deleteAll(page2.getContent());
		} else {
			log.info("2Concept deleteAll size=0");
		}
		log.info("2Concept deleteAll done");
	}
}
