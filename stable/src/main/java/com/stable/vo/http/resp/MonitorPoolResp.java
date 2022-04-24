package com.stable.vo.http.resp;

import com.stable.vo.bus.MonitorPoolTemp;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MonitorPoolResp extends MonitorPoolTemp {
	private static final long serialVersionUID = 1;
	private String codeName;
	private String monitorDesc;
	private String ykbDesc;
}
