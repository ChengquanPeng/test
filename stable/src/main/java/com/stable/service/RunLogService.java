package com.stable.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsRunLogDao;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.RunLog;
import com.stable.vo.http.resp.RunLogResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 日志
 * 
 * @author roy
 *
 */
@Service("RunLogService")
@Log4j2
public class RunLogService {
	@PostConstruct
	public void dostart() {
		WxPushUtil.pushSystem1("系统正常启动");
		// new RuntimeException().printStackTrace();
	}

	@Autowired
	private EsRunLogDao runlogDao;

	private Date createDate = DateUtil.parseDate("2099-12-31 23:23:59", DateUtil.YYYY_MM_DD_HH_MM_SS);

	public void addLog(RunLog log) {
		runlogDao.save(log);
		if (log.getRunCycle() != RunCycleEnum.MANUAL.code) {
			log.setId(String.valueOf(log.getBtype()));
			log.setCreateDate(createDate);
			runlogDao.save(log);
		}
	}

	public List<RunLogResp> queryRunlogs(Integer btype, Integer date, EsQueryPageReq queryPage) {
		Pageable pageable = PageRequest.of(queryPage.getPageNum(), queryPage.getPageSize());
		log.info("page btype={},date={},pageable.page={},pageable.size={}", btype, date, pageable.getPageNumber(),
				pageable.getPageSize());
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		FieldSortBuilder sort = SortBuilders.fieldSort("createDate").unmappedType("integer").order(SortOrder.DESC);

		if (btype != null && btype.intValue() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("btype", btype));
		}
		if (date != null && date.intValue() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		}

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		List<RunLog> result = runlogDao.search(sq).getContent();
		return switchValue(result);
	}

	private List<RunLogResp> switchValue(List<RunLog> result) {
		List<RunLogResp> list = new LinkedList<RunLogResp>();
		if (result != null) {
			for (RunLog rl : result) {
				RunLogResp rlp = new RunLogResp();
				BeanUtils.copyProperties(rl, rlp);
				rlp.setBtypeName(RunLogBizTypeEnum.getRunLogBizTypeEnum(rl.getBtype()).getBtypeName());
				rlp.setCycleName(RunCycleEnum.getRunCycleEnum(rl.getRunCycle()).getName());
				if (rl.getStatus() == 1) {
					rlp.setStatusName("已完成");
				} else if (rl.getStatus() == 2) {
					rlp.setStatusName("异常");
				} else {
					rlp.setStatusName("未完成");
				}
				list.add(rlp);
			}
		}
		return list;
	}
}
