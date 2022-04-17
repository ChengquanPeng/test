package com.stable.service.monitor;

import org.apache.commons.lang3.StringUtils;

import com.stable.utils.DateUtil;

import lombok.Data;

@Data
public class RealtimeMsg {

	private static final String BLANK = " ";
	private String code;
	private String codeName;
	private String msg;
	private String firstTimeWarning;
	private int times;

	public void tiggerMessage() {
		// 1
		if (StringUtils.isBlank(firstTimeWarning)) {
			firstTimeWarning = DateUtil.getTodayYYYYMMDDHHMMSS();
		}
		times++;
	}

	public String toMessage() {
		StringBuffer sb = new StringBuffer();
		sb.append("关注:").append(code).append(BLANK).append(codeName).append(BLANK);//
		sb.append(",模型版本:").append(msg).append(BLANK)//
				.append(",第一次提醒时间:").append(firstTimeWarning).append(BLANK)//
				.append(",提醒次数:").append(times).append(BLANK);//
		return sb.toString();
	}
}
