package com.stable.vo.http.resp;

import com.stable.vo.bus.DividendHistory;

import lombok.Data;
@Data
public class DividendHistoryResp extends DividendHistory {

	private String codeName;
}
