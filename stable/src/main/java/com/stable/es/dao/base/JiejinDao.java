package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.Jiejin;

@Repository
public interface JiejinDao extends ElasticsearchRepository<Jiejin, String> {

}
