package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.CodeBaseModel2;

@Repository
public interface EsCodeBaseModel2Dao extends ElasticsearchRepository<CodeBaseModel2, String> {

}
