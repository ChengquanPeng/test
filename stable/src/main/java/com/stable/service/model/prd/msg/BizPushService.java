package com.stable.service.model.prd.msg;

import java.util.LinkedList;
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

	List<UserInfo> users = new LinkedList<UserInfo>();

	public void initUser() {
		users = userService.getUserListForMonitorS2();
	}

	public void removeUser() {
		users = new LinkedList<UserInfo>();
	}

	public boolean pushS2Realtime(String title, String msg) {
		return MsgPushServer.pushSystemHtmlBatch(title, msg, userService.getUserListForMonitorS2());
	}

	public boolean pushS2ForTradeTime(String title, String msg) {
		return MsgPushServer.pushSystemHtmlBatch(title, msg, users);
	}

}
