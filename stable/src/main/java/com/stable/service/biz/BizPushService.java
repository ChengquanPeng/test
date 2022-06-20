package com.stable.service.biz;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.UserInfo;

/**
 * 商用消息推送
 */
@Service
public class BizPushService {

	@Autowired
	private UserService userService;

	public void PushS2(String msg) {
		List<UserInfo> ulist = userService.getUserListForMonitorS2();
		for (UserInfo u : ulist) {
			WxPushUtil.pushSystem2Html(u.getWxpush(), msg);
		}
	}
}
