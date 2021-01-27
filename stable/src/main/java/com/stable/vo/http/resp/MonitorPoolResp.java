package com.stable.vo.http.resp;

import com.stable.vo.bus.MonitorPool;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MonitorPoolResp extends MonitorPool {

	private String codeName;
	private String monitorDesc;
	private String ykbDesc;
}
