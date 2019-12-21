package com.stable.service;

import java.util.LinkedList;
import java.util.List;

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
import com.stable.es.dao.EsRunLogDao;
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

	@Autowired
	private EsRunLogDao runlogDao;

	public void addLog(RunLog log) {
		runlogDao.save(log);
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
				rlp.setBtypeName(RunLogBizTypeEnum.BUY_BACK.getRunLogBizTypeEnum(rl.getBtype()).getBtypeName());
				rlp.setCycleName(RunCycleEnum.DAY.getRunCycleEnum(rl.getRunCycle()).getName());
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
