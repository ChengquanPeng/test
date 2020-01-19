package com.stable.utils;

import java.util.HashSet;
import java.util.Set;

import com.stable.config.SpringConfig;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class WxPushUtil {

	private static String appToken;
	private static String myUid;
	static {
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		appToken = efc.getAppToken();
		myUid = efc.getMyUid();
	}

	public final static void pushSystem1(String content) {
		WxPushUtil.pushMsg(content, true, null);
	}

	private final static void pushMsg(String content, boolean isMyId, Set<String> uids) {
		try {
			Message message = new Message();
			message.setAppToken(appToken);
			message.setContentType(Message.CONTENT_TYPE_TEXT);
			message.setContent(content + " 时间:" + DateUtil.getTodayYYYYMMDDHHMMSS());
			if (isMyId) {
				message.setUid(myUid);
			} else {
				message.setUids(uids);
			}
			message.setUrl(null);
			log.info("微信推送内容:{},状态:{}", content, WxPusher.send(message).getData().get(0).getStatus());
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "微信推送内容异常", content, "");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Set<String> uids = new HashSet<String>();
		uids.add("UID_QNfInsOVwTvTsYTQsjJB2KUL52VI");
		uids.add("UID_HgYWkLePDYzlvAhTMlD4AaUCEUhl");
		Message message = new Message();
		message.setAppToken("AT_X5jTnRko4rhQ4dmoS7ntwBN79BxATk3r");
		message.setContentType(Message.CONTENT_TYPE_TEXT);
		message.setContent("不加限制的自由是很可怕的，因为很容易让任何人滑向深渊。-- 彭成全 " + DateUtil.getTodayYYYYMMDDHHMMSS());
		message.setUids(uids);
		message.setUrl(null);
		System.err.println(WxPusher.send(message).getData().get(0).getStatus());
	}
}
