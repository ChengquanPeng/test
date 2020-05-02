package com.stable.vo.http.resp;

import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class DaliyTradeHistResp extends TradeHistInfoDaliy {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4762807147245908817L;
	private String codeName;
}
