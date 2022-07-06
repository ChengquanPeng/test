package com.stable.service.biz;

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

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.Stype;
import com.stable.es.dao.base.UserAmtLogDao;
import com.stable.es.dao.base.UserDao;
import com.stable.msg.WxPushUtil;
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

	public List<UserInfo> getUserListForMonitorS1() {
		List<UserInfo> r = new LinkedList<UserInfo>();
		int today = DateUtil.getTodayIntYYYYMMDD();
		UserInfo q = new UserInfo();
		q.setS1(today);
		List<UserInfo> l = this.getList(q, EsQueryPageUtil.queryPage9999);
		if (l != null) {
			for (UserInfo u : l) {
				if (StringUtils.isNotBlank(u.getWxpush())) {
					r.add(u);
				}
			}
		}
		UserInfo myid = new UserInfo();
		myid.setId(Constant.MY_ID);
		myid.setWxpush(WxPushUtil.myUid);
		r.add(myid);
		return r;
	}

	public List<UserInfo> getUserListForMonitorS2() {
		List<UserInfo> r = new LinkedList<UserInfo>();
		int today = DateUtil.getTodayIntYYYYMMDD();
		UserInfo q = new UserInfo();
		q.setS2(today);
		List<UserInfo> l = this.getList(q, EsQueryPageUtil.queryPage9999);
		if (l != null) {
			for (UserInfo u : l) {
				if (StringUtils.isNotBlank(u.getWxpush())) {
					r.add(u);
				}
			}
		}
		UserInfo myid = new UserInfo();
		myid.setId(Constant.MY_ID);
		myid.setWxpush(WxPushUtil.myUid);
		r.add(myid);
		return r;
	}

	// 查询
	public List<UserInfo> getList(UserInfo user, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (user.getId() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("id", user.getId()));
		}
		if (user.getS1() > 0) {
			bqb.must(QueryBuilders.rangeQuery("s1").gte(user.getS1()));
		}
		if (user.getS2() > 0) {
			bqb.must(QueryBuilders.rangeQuery("s2").gte(user.getS2()));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("createDate").unmappedType("integer").order(SortOrder.ASC);

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

	public List<UserInfo> getListForServiceEnd(int stype, int start, int end, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (stype == 1) {
			bqb.must(QueryBuilders.rangeQuery("s1").gte(start).lte(end));
		}
		if (stype == 2) {
			bqb.must(QueryBuilders.rangeQuery("s2").gte(start).lte(end));
		}

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<UserInfo> page = userDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no UserInfo for getListForWarning");
		return null;
	}

	public UserInfo getListById(long id) {
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

	// 修改信息
	public synchronized void updateinfo(UserInfo user) {
		UserInfo exs = getListById(user.getId());
		if (exs == null) {
			throw new RuntimeException("用户不存在user=" + user.getId());
		} else {
			exs.setName(user.getName());
			exs.setRemark(user.getRemark());
			exs.setWxpush(user.getWxpush());
			userDao.save(exs);
		}
	}

	// 修改信息
	public synchronized void lastLogin(long id) {
		UserInfo exs = getListById(id);
		if (exs == null) {
			throw new RuntimeException("用户不存在user=" + id);
		} else {
			exs.setLastLogin(DateUtil.getTodayYYYYMMDDHHMMSS());
			userDao.save(exs);
		}
	}

	// 新增
	public synchronized void add(UserInfo user) {
		UserInfo exs = getListById(user.getId());
		if (exs == null) {
			user.setCreateDate(DateUtil.getTodayIntYYYYMMDD());
			userDao.save(user);
			log.info("save user req={}", user);
		} else {
			log.warn("用户已经存在 user  req={}", user);
			throw new RuntimeException("用户已经存在 user  req=" + user);
		}
	}

	// 正常充值
	public synchronized void update(long userid, double amt, int stype, int month) {
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
			amtlog.setUid(userid);
			amtlog.setDate(today);
			amtlog.setLogId(userid + "|" + amtlog.getDate() + "|" + System.currentTimeMillis());

			if (stype == Stype.webQuery.getCode()) {
				amtlog.setStype(stype);
				int sd = exs.getS1();
				amtlog.setExeStartDate(sd);
				int expiredDate = getExpiredDate(sd, month);
				exs.setS1(expiredDate);
				amtlog.setExpiredDate(expiredDate);
				exs.setRemark(today + "-s1充值" + amt + "元," + month + "月,计算日=" + sd + ",过期=" + expiredDate);
			} else if (stype == Stype.sysChance.getCode()) {
				amtlog.setStype(stype);
				int sd = exs.getS2();
				amtlog.setExeStartDate(sd);
				int expiredDate = getExpiredDate(sd, month);
				exs.setS2(expiredDate);
				amtlog.setExpiredDate(expiredDate);
				exs.setRemark(today + "-s2充值" + amt + "元," + month + "月,计算日=" + sd + ",过期=" + expiredDate);
			} else {
				throw new RuntimeException("stype=" + userid);
			}
			amtlog.setAmt(amt);
			userAmtLogDao.save(amtlog);
			// ===
			log.info("old info:{}", exs);
			if (exs.getMemDate() == 0) {
				exs.setMemDate(today);
			}
			userDao.save(exs);
			log.info("save user new info={}", exs);
		}
	}

	// 人工修改
	public synchronized void manulUpdate(long id, int stype, int days) {
		if (days == 0) {
			throw new RuntimeException("days=0");
		}
		UserInfo exs = getListById(id);
		if (exs == null) {
			log.warn("用户不存在 req={}", id);
			throw new RuntimeException("用户不存在 req=" + id);
		} else {
			int today = DateUtil.getTodayIntYYYYMMDD();
			if (stype == Stype.webQuery.getCode()) {
				int sd = exs.getS1();
				int expiredDate = getExpiredDateByDays(exs.getS1(), days);
				exs.setS1(expiredDate);
				exs.setRemark(today + "-s1人工增加" + days + "天,计算日=" + sd + ",过期=" + expiredDate);
			} else if (stype == Stype.sysChance.getCode()) {
				int sd = exs.getS2();
				int expiredDate = getExpiredDateByDays(exs.getS2(), days);
				exs.setS2(expiredDate);
				exs.setRemark(today + "-s2人工增加" + days + "天,计算日=" + sd + ",过期=" + expiredDate);
			} else {
				throw new RuntimeException("stype=" + id);
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

	/*
	 * type:服务类型, msg:服务,
	 */
	public synchronized void sendMsg(int type, String msg) {
		if (type == 1) {// 所有在线服务1

		}
		if (type == 2) {// 所有在线服务2

		}
		if (type == 9) {// 所有服务

		}
	}
}
