package com.stable.service.model.prd.msg;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.stable.constant.Constant;
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
		email.pushSystemT1(content, content, email.myId);
		WxPushUtil.pushSystemT1(content, WxPushUtil.myUid);
		return true;
	}

	public final static boolean pushSystemT1(String title, String content) {
		email.pushSystemT1(title, content, email.myId);
		WxPushUtil.pushSystemT1(title + content, WxPushUtil.myUid);
		return true;
	}

	public final static boolean pushSystemHtmlT2(String title, String content) {
		email.pushSystemHtmlT2(title, content, email.myId);
		WxPushUtil.pushSystemHtmlT2(title + content, WxPushUtil.myUid);
		return true;
	}

	/** --管理员推送-- */

	/** --单个客户推送-- */
	public final static boolean pushSystemT1(String title, String content, UserInfo user) {
		int r = getPushWay(user);
		if (r == 9) {
			email.pushSystemHtmlT2(title, content, user.getQqmail());
			WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
			return true;
		} else if (r == 2) {
			return email.pushSystemHtmlT2(title, content, user.getQqmail());
		} else {
			return WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
		}
	}

	public final static boolean pushSystemHtmlT2(String title, String content, UserInfo user) {
		int r = getPushWay(user);
		if (r == 9) {
			email.pushSystemHtmlT2(title, content, user.getQqmail());
			WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
			return true;
		} else if (r == 2) {
			return email.pushSystemHtmlT2(title, content, user.getQqmail());
		} else {
			return WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
		}
	}

	/** --单个客户推送-- */

	/** --多个客户推送-- */
	public final static boolean pushSystemHtmlBatch(String title, String content, List<UserInfo> users) {
		List<String> l = new LinkedList<String>();
		for (UserInfo user : users) {
			int r = getPushWay(user);
			if (r == 9) {
				l.add(user.getQqmail());
				WxPushUtil.pushSystemHtmlT2(title + content, user.getWxpush());
			} else if (r == 2) {
				l.add(user.getQqmail());
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

	private final static int getPushWay(UserInfo u) {
		if (u.getId() == Constant.MY_ID) {
//			wxpush = MsgPushServer.email.myId;
//			return true;
			u.setWxpush(WxPushUtil.myUid);
			u.setQqmail(email.myId);
			return 9;
		} else if (StringUtils.isNotBlank(u.getWxpush())) {
			return 1;
		} else {
			u.setQqmail(u.getId() + MsgPushServer.qqmail);
			return 2;
		}
	}
}
