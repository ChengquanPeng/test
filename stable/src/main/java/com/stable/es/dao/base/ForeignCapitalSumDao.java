package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.ForeignCapitalSum;

@Repository
public interface ForeignCapitalSumDao extends ElasticsearchRepository<ForeignCapitalSum, String> {

}
