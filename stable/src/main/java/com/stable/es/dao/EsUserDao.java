package com.stable.es.dao;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.es.vo.EsUser;

@Repository
public interface EsUserDao extends ElasticsearchRepository<EsUser, Long>{

}
