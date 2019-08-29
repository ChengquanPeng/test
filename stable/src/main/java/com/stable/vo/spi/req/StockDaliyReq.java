package com.stable.vo.spi.req;

import lombok.Data;

@Data
public class StockDaliyReq {
	private String ts_code;
	private String start_date;
	private String end_date;
	private String trade_date;
}
