package com.stable.service.model.prd.msg;

import java.util.LinkedList;
import java.util.List;

import com.stable.utils.SpringUtil;
import com.stable.vo.bus.UserInfo;

public class MsgPushServer {
	public static final String qqmail = "@qq.com";

	public static SendEamilService email;
	static {
		try {
			email = SpringUtil.getBean(SendEamilService.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** --管理员推送-- */
	public final static boolean pushSystem1(String content) {
		if (!WxPushUtil.pushSystemT1(content, WxPushUtil.myUid)) {
			return email.pushSystemT1(content, content, email.myId);
		}
		return true;
	}

	public final static boolean pushSystemT1(String title, String content) {
		if (!WxPushUtil.pushSystemT1(title + content, WxPushUtil.myUid)) {
			return email.pushSystemT1(title, content, email.myId);
		}
		return true;
	}

	public final static boolean pushSystemHtmlT2(String title, String content) {
		if (!WxPushUtil.pushSystemHtmlT2(title + content, WxPushUtil.myUid)) {
			return email.pushSystemHtmlT2(title, content, email.myId);
		}
		return true;
	}

	/** --管理员推送-- */

	/** --单个客户推送-- */
	public final static boolean pushSystemT1(String title, String content, UserInfo user) {
		if (user.getPushWay()) {
			return email.pushSystemT1(title, content, user.getWxpush());
		} else {
			return WxPushUtil.pushSystemT1(title + content, user.getWxpush());
		}
	}

	public final static boolean pushSystemHtmlT2(String title, String content, UserInfo user) {
		if (user.getPushWay()) {
			return email.pushSystemHtmlT2(title, content, user.getWxpush());
		} else {
			return WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
		}
	}

	/** --单个客户推送-- */

	/** --多个客户推送-- */
	public final static boolean pushSystemHtmlBatch(String title, String content, List<UserInfo> users) {
		List<String> l = new LinkedList<String>();
		for (UserInfo user : users) {
			if (user.getPushWay()) {
				l.add(user.getWxpush());
			} else {
				WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
			}
		}
		if (l.size() > 0) {
			String[] us = new String[l.size()];
			email.pushSystemHtmlT2(title, content, l.toArray(us));
		}
		return true;
	}
	/** --多个客户推送-- */
}
