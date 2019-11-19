package com.stable.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.db.dao.UserDao;
import com.stable.es.dao.EsUserDao;
import com.stable.vo.UserVo;
import com.stable.vo.bus.EsUser;

@Service
public class UserService {

	@Autowired
    private UserDao userDao;
	
	@Autowired
	private EsUserDao esUserDao;
	
	private void atest() {
		System.err.println(this.getUserById(1));
	}
	
	@Cacheable(value = "userinfo", key = "#id", unless="#result == null")
    public UserVo getUserById(Integer id) {
    	/*String key = "test";
    	String value = redisUtil.get(key);
    	if(StringUtils.isNotBlank(value)) {
    		System.err.println("from cache..");
    		return JSON.parseObject(value, UserVo.class);
    	}*/
		System.err.println("db...");
    	UserVo vo = userDao.getUserById(id);
    	//redisUtil.set(key, vo);
    	//vo.setCtm(null);
    	return vo;
    }
    public List<UserVo> getUserList() {
        return userDao.getUserList();
    }
    public int add(UserVo user) {
        return userDao.add(user);
    }
    public int update(Integer id, UserVo user) {
        return userDao.update(id, user);
    }
    public int delete(Integer id) {
        return userDao.delete(id);
    }
    
    private void esTest() {
    	esUserDao.deleteAll();
    	EsUser user = new EsUser();
    	user.setId("10");
    	user.setUsername("username中午");
    	user.setAge("10");
    	user.setCtm(new Date());
    	esUserDao.save(user);
    	
    	user.setId("11");
    	user.setUsername("username中午2");
    	user.setAge("10");
    	user.setCtm(new Date());
    	esUserDao.save(user);
    	
    	Optional<EsUser> db = esUserDao.findById("10");
    	System.err.println(db.isPresent()?db.get():"null");
    	
    	SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("age", "10"))).build();
    	Page<EsUser> page = esUserDao.search(searchQuery);
    	System.err.println(page.getContent());
    	//esTemplate.qu
    }
    
    @Autowired
    private ElasticsearchTemplate esTemplate;
}
