package com.stable.vo.spi.req;

import lombok.Data;

@Data
public class DividendReq {
	private String ts_code;// TS代码
	private String ann_date;// 公告日
	private String record_date;// 股权登记日期
	private String ex_date;// 除权除息日
	private String imp_ann_date;// 实施公告日

}
