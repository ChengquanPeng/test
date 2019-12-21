package com.stable.vo;

import lombok.Data;

@Data
public class MarketHistroyVo {
	private String ts_code;
	private String start_date;
	private String end_date;
	private String adj;
	private String freq;
}
