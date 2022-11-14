package com.stable.service.datamv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import com.stable.vo.spi.req.EsQueryPageReq;

@Service

public class DataMovingProvider {
	@Autowired
	private DataMovingNewer dataMovingNewer;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Dw getData(String tableName, int pageNum, int pageSize) {
		tableName = tableName.trim();
		Dw dw = new Dw();
		dw.setTableName(tableName);
		ElasticsearchRepository dao = dataMovingNewer.daoMap.get(tableName);
		if (dao != null) {
			if (pageNum <= 1) {
				dw.setTableSize(dao.count());
			}
			EsQueryPageReq req = new EsQueryPageReq();
			req.setPageNum(pageNum);
			req.setPageSize(pageSize);
			Pageable pageable = PageRequest.of(req.getPageNum(), req.getPageSize());
			Page p = dao.findAll(pageable);
			if (p != null && p.getContent() != null && p.getContent().size() > 0) {
				dw.setTableData(p.getContent());
				dw.setBatchSize(dw.getTableData().size());
			}
		}
		return dw;
	}

}
