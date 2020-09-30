package com.stable.service.realtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.stable.utils.DateUtil;

import lombok.Data;

@Data
public class RealtimeMsg {

	private static final String BLANK = " ";
	private String code;
	private String codeName;
	private int baseScore;

	public void addMessage(String verAndSubVer) {
		// 1
		m_ver.add(verAndSubVer);
		// 2
		String firstTimeWarning = m_firstTimeWarning.get(verAndSubVer);
		if (StringUtils.isBlank(firstTimeWarning)) {
			m_firstTimeWarning.put(verAndSubVer, DateUtil.getTodayYYYYMMDDHHMMSS());
		}

		// 3
		Integer times = m_times.get(verAndSubVer);
		if (times == null) {
			times = 1;
		} else {
			times = times + 1;
		}
		m_times.put(verAndSubVer, times);
	}

	// V-
	private Set<String> m_ver = new HashSet<String>();
	private Map<String, String> m_firstTimeWarning = new HashMap<String, String>();
	private Map<String, Integer> m_times = new HashMap<String, Integer>();

	public String toMessage() {
		StringBuffer sb = new StringBuffer();
		sb.append("关注:").append(code).append(BLANK).append(codeName).append(BLANK)//
				.append(",基本评分:").append(baseScore).append(BLANK);//
		for (String ver : m_ver) {
			sb.append(",模型版本:V").append(ver).append(BLANK)//
					.append(",第一次提醒时间:").append(m_firstTimeWarning.get(ver)).append(BLANK)//
					.append(",提醒次数:").append(m_times.get(ver)).append(BLANK);//
		}
		return sb.toString();
	}
}
