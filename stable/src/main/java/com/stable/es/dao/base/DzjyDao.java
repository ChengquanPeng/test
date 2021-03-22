package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.Dzjy;

@Repository
public interface DzjyDao extends ElasticsearchRepository<Dzjy, String> {

}
