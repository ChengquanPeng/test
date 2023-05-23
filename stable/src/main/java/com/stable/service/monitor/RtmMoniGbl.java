package com.stable.service.monitor;

import java.util.LinkedList;
import java.util.List;

import com.stable.constant.Constant;
import com.stable.service.model.prd.msg.BizPushService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.TagUtil;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.http.resp.CodeBaseModelResp;

import lombok.Data;

@Data
public class RtmMoniGbl {

	// 常规监听
	private List<RtmMoniUser> listu = new LinkedList<RtmMoniUser>();

	// 公共监听
	public MonitorPoolTemp orig;
	public BizPushService bizPushService;
	public double warningYellow = 0.0;
	public boolean warningYellowChk = false;
	public String you = "";
	public double price3mYellow = 0.0;;
	public boolean price3mYellowChk = false;
	// 公共信息
	public CodeBaseModelResp base;

	public void addUser() {

	}

	public RtmMoniGbl(CodeBaseModelResp resp) {
		this.base = resp;
	}

	public void setServiceAndPrew(BizPushService bizs, MonitorPoolTemp orig) {
		this.orig = orig;
		this.bizPushService = bizs;
		if (base.getPls() == 1) {
			price3mYellow = CurrencyUitl.roundHalfUp(orig.getXpPrice() * 0.97);
			price3mYellowChk = true;
			warningYellow = CurrencyUitl.roundHalfUp((orig.getShotPointPrice() * 0.98));
			warningYellowChk = true;
		}
		you = TagUtil.getTag(base);
	}

	public String getMsg(MonitorPoolTemp u) {
		if (u.getUserId() == Constant.MY_ID) {
			return base.getBuyRea() + Constant.HTML_LINE + base.getZfjjInfo() + base.getRengong();
		} else {
			return u.getMsg() == null ? "" : u.getMsg();
		}
	}
}
