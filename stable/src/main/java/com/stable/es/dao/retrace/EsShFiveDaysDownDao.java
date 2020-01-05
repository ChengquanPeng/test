package com.stable.es.dao.retrace;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.up.strategy.ShFiveDaysDown;

@Repository
public interface EsShFiveDaysDownDao extends ElasticsearchRepository<ShFiveDaysDown, String>{

}
