package com.stable.vo.http.resp;

import lombok.Data;

@Data
public class DzjyResp {
	private String code;
	private int date = 0;
	private String totalAmts;// 总额-1年
	private double avgPrcie;// 均价-1年
	private String totalAmt60ds;// 总额-60天
	private String codeName;
}
