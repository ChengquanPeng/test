package com.stable.service;

import java.util.List;

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

import com.stable.enums.Stype;
import com.stable.es.dao.base.UserAmtLogDao;
import com.stable.es.dao.base.UserDao;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.UserAmtLog;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 用户及充值管理
 */
@Service
@Log4j2
public class UserService {
	@Autowired
	private UserDao userDao;
	@Autowired
	private UserAmtLogDao userAmtLogDao;

	// 查询
	public List<UserInfo> getList(UserInfo user, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (user.getId() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("id", user.getId()));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("rptDate").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<UserInfo> page = userDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no UserInfo for req={}", user);
		return null;
	}

	public UserInfo getListById(int id) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (id > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("id", id));
		} else {
			throw new RuntimeException("id is null");
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<UserInfo> page = userDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	// 新增
	public synchronized void add(UserInfo user) {
		UserInfo exs = getListById(user.getId());
		if (exs == null) {
			userDao.save(user);
			log.info("save user req={}", user);
		} else {
			log.warn("用户已经存在 user  req={}", user);
			throw new RuntimeException("用户已经存在 user  req=" + user);
		}
	}

	// 正常充值
	public synchronized void update(int userid, double amt, int stype, int month) {
		if (amt <= 0) {
			throw new RuntimeException("充值金额=" + amt);
		}
		if (month <= 0) {
			throw new RuntimeException("month=" + month);
		}
		UserInfo exs = getListById(userid);
		if (exs == null) {
			log.warn("用户不存在 req={}", userid);
			throw new RuntimeException("用户不存在 req=" + userid);
		} else {
			int today = DateUtil.getTodayIntYYYYMMDD();
			UserAmtLog amtlog = new UserAmtLog();
			amtlog.setDate(today);
			amtlog.setLogId(userid + "|" + amtlog.getDate() + "|" + System.currentTimeMillis());

			if (stype == Stype.webQuery.getCode()) {
				amtlog.setStype(stype);
				int expiredDate = getExpiredDate(exs.getS1(), month);
				exs.setS1(expiredDate);
				amtlog.setExpiredDate(expiredDate);
				exs.setRemark(today + "充值 add S1 " + month + "月,过期=" + expiredDate);
			} else if (stype == Stype.sysChance.getCode()) {
				amtlog.setStype(stype);
				int expiredDate = getExpiredDate(exs.getS2(), month);
				exs.setS2(expiredDate);
				amtlog.setExpiredDate(expiredDate);
				exs.setRemark(today + "充值 add S2 " + month + "月,过期=" + expiredDate);
			} else {
				throw new RuntimeException("stype=" + userid);
			}
			amtlog.setAmt(amt);
			userAmtLogDao.save(amtlog);
			// ===
			log.info("old info:{}", exs);
			userDao.save(exs);
			log.info("save user new info={}", exs);
		}
	}

	// 人工修改
	public synchronized void manulUpdate(int userid, int stype, int days) {
		if (days <= 0) {
			throw new RuntimeException("days=" + days);
		}
		UserInfo exs = getListById(userid);
		if (exs == null) {
			log.warn("用户不存在 req={}", userid);
			throw new RuntimeException("用户不存在 req=" + userid);
		} else {
			int today = DateUtil.getTodayIntYYYYMMDD();
			if (stype == Stype.webQuery.getCode()) {
				int expiredDate = getExpiredDateByDays(exs.getS1(), days);
				exs.setS1(expiredDate);
				exs.setRemark(today + "人工 add S1 " + days + "天,过期=" + expiredDate);
			} else if (stype == Stype.sysChance.getCode()) {
				int expiredDate = getExpiredDateByDays(exs.getS2(), days);
				exs.setS2(expiredDate);
				exs.setRemark(today + "人工 add S2 " + days + "天,过期=" + expiredDate);
			} else {
				throw new RuntimeException("stype=" + userid);
			}
			log.info("old info:{}", exs);
			userDao.save(exs);
			log.info("save user new info={}", exs);
		}
	}

	private int getExpiredDate(int endDate, int month) {
		int today = DateUtil.getTodayIntYYYYMMDD();
		int startdate = today;
		if (endDate > today) {// 如果服务日期大于今天，按最大的服务日期开始计算
			startdate = endDate;
		}
		return DateUtil.addMonth(startdate, month);
	}

	private int getExpiredDateByDays(int endDate, int days) {
		int today = DateUtil.getTodayIntYYYYMMDD();
		int startdate = today;
		if (endDate > today) {// 如果服务日期大于今天，按最大的服务日期开始计算
			startdate = endDate;
		}
		return DateUtil.addDate(startdate, days);
	}

	// 定时任务-通知和终止
}
