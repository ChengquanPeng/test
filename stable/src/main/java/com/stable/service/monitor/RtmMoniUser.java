package com.stable.service.monitor;

import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.UserInfo;

import lombok.Data;

@Data
public class RtmMoniUser {
	public MonitorPoolTemp orig;
	public UserInfo user;
	public boolean waitSend = true;

	public RtmMoniUser(MonitorPoolTemp cp, UserInfo user) {
		this.orig = cp;
		this.user = user;
	}
}
