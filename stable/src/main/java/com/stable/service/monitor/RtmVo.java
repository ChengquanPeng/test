package com.stable.service.monitor;

import com.stable.constant.Constant;
import com.stable.enums.MonitorType;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.http.resp.CodeBaseModelResp;

import lombok.Data;

@Data
public class RtmVo {
	public MonitorPoolTemp orig;
	public String msg;
	public String wxpush;
	public boolean waitSend = true;
	public boolean highPriceGot = false;

	public RtmVo(MonitorPoolTemp cp, CodeBaseModelResp cbm) {
		this.orig = cp;
		String msgt = "";
		msgt += MonitorType.getCodeName(cp.getMonitor()) + "!" + cp.getRemark();
		if (cp.getUserId() == Constant.MY_ID && cbm.getPls() == 1) {
			msgt += " " + cbm.getZfjjInfo();
		}
		msgt += " " + cp.getMsg();
		msg = msgt;
	}

}
