package com.stable.vo.http.resp;

import com.stable.vo.bus.TickDataBuySellInfo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TickDataBuySellInfoResp extends TickDataBuySellInfo {
	/**
	 * 
	 */
	private static final long serialVersionUID = 14245721862668962L;

	private String codeName;
	
	private String buyTotalAmt1;
	private String sellTotalAmt1;
	private String totalAmt1;
}
