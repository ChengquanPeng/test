package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.up.strategy.ModelV1;

@Repository
public interface EsModelV1Dao extends ElasticsearchRepository<ModelV1, String>{

}
