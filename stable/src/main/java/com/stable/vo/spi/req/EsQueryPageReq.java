package com.stable.vo.spi.req;

import lombok.Data;

@Data
public class EsQueryPageReq {
	public Integer getPageNum() {
		if (pageNum == null || pageNum <= 1) {
			pageNum = 1;
		}
		return pageNum;
	}

	public Integer getPageSize() {
		if (pageSize == null || pageSize <= 0) {
			pageSize = 20;
		}
		return pageSize;
	}

	Integer pageNum = 0;
	Integer pageSize = 20;

}
