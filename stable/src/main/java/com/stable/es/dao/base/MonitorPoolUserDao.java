package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.MonitorPoolTemp;

@Repository
public interface MonitorPoolUserDao extends ElasticsearchRepository<MonitorPoolTemp, String>{

}
