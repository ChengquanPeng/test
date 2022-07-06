package com.stable.msg;

import com.stable.utils.SpringUtil;

public class MsgPushServer {
	public static boolean isWxPush = true;
	public static SendEamilService email;
	static {
		try {
			email = SpringUtil.getBean(SendEamilService.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getMyId() {
		if (isWxPush) {
			return WxPushUtil.myUid;
		} else {
			return email.myId;
		}
	}

	public final static boolean pushSystem1(String content) {
		return pushSystemWithTitle("", content);
	}

	public final static boolean pushSystemWithTitle(String title, String content) {
		if (isWxPush) {
			return WxPushUtil.pushSystem1(content);
		} else {
			return email.pushSystem1(title, content);
		}
	}

	public final static boolean pushSystem1(String toId, String content) {
		return pushSystemWithTitle("", content, toId);
	}

	public final static boolean pushSystemWithTitle(String title, String content, String... toId) {
		if (isWxPush) {
			return WxPushUtil.pushSystem1(toId[0], content);
		} else {
			return email.pushSystem1(title, content, toId);
		}
	}

	public final static boolean pushSystem2Html(String title, String content, String... toId) {
		if (isWxPush) {
			return WxPushUtil.pushSystem2Html(toId[0], content);
		} else {
			return email.pushSystem2Html(title, content, toId);
		}

	}

	public final static boolean pushSystem2Html(String content) {
		return pushSystem2Html("", content);
	}

	public final static boolean pushSystem2Html(String title, String content) {
		if (isWxPush) {
			return WxPushUtil.pushSystem2Html(content);
		} else {
			return email.pushSystem2Html(title, content);
		}
	}

}
