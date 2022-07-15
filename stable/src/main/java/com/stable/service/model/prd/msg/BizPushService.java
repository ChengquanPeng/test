package com.stable.service.model.prd.msg;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.model.prd.UserService;
import com.stable.vo.bus.UserInfo;

/**
 * 商用消息推送
 */
@Service
public class BizPushService {

	@Autowired
	private UserService userService;

	public boolean PushS2(String title, String msg) {
		List<UserInfo> users = userService.getUserListForMonitorS2();
		return MsgPushServer.pushSystemHtmlBatch(title, msg, users);
	}

}