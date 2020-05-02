package com.stable.vo.http.resp;

import com.stable.vo.bus.BuyBackInfo;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class BuyBackInfoResp extends BuyBackInfo {

	/**
	 * 
	 */
	private static final long serialVersionUID = -595249103482156234L;
	private String codeName;
	private String amountStr;
}
