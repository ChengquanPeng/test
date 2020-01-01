package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.TradeCal;

@Repository
public interface EsTradeCalDao extends ElasticsearchRepository<TradeCal, Integer>{

}
