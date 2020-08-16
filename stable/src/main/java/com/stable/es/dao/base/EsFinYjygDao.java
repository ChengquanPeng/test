package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.FinYjyg;

@Repository
public interface EsFinYjygDao extends ElasticsearchRepository<FinYjyg, String>{

}
