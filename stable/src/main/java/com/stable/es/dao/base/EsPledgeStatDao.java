package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.PledgeStat;

@Repository
public interface EsPledgeStatDao extends ElasticsearchRepository<PledgeStat, String>{

}
