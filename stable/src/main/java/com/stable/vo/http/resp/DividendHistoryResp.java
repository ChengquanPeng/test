package com.stable.vo.http.resp;

import com.stable.vo.bus.DividendHistory;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class DividendHistoryResp extends DividendHistory {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2868909418803170382L;
	private String codeName;
}
