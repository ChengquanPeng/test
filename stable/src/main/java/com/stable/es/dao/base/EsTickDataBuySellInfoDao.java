package com.stable.es.dao.base;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.stable.vo.bus.TickDataBuySellInfo;

@Repository
public interface EsTickDataBuySellInfoDao extends ElasticsearchRepository<TickDataBuySellInfo, String>{

}
