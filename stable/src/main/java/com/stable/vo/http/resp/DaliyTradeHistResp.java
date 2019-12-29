package com.stable.vo.http.resp;

import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.Data;
@Data
public class DaliyTradeHistResp extends TradeHistInfoDaliy {

	private String codeName;
}
