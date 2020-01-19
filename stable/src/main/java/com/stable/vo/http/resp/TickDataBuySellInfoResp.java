package com.stable.vo.http.resp;

import com.stable.vo.bus.TickDataBuySellInfo;

import lombok.Data;

@Data
public class TickDataBuySellInfoResp extends TickDataBuySellInfo {
	private String codeName;
	
	private String buyTotalAmt1;
	private String sellTotalAmt1;
	private String totalAmt1;
}
