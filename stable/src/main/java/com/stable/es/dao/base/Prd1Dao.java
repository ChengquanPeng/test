package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.Prd1;

@Repository
public interface Prd1Dao extends ElasticsearchRepository<Prd1, String> {

}
