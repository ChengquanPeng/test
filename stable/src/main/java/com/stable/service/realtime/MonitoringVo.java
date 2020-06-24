package com.stable.service.realtime;

import com.stable.enums.MonitoringType;

import lombok.Data;

@Data
public class MonitoringVo {

	private String code;
	private int date;
	private MonitoringType mt;
}
