package com.stable.vo.http.resp;

import com.stable.vo.bus.RunLog;

import lombok.Data;

@Data
public class RunLogResp extends RunLog {
	private String id;
	private int date;
	private int btype;
	private int status;
	private int runCycle;
	private String startTime;
	private String endTime;
	private String remark;
	
	private String btypeName;
	private String cycleName;
	private String statusName;
}
