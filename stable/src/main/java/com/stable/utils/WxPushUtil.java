package com.stable.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stable.config.SpringConfig;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import com.zjiecode.wxpusher.client.bean.MessageResult;
import com.zjiecode.wxpusher.client.bean.Result;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class WxPushUtil {

	private static String appToken;
	private static String myUid;
	private static String env;
	static {
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		appToken = efc.getAppToken().trim();
		myUid = efc.getMyUid().trim();
		env = " from " + efc.getEnv().trim() + " ";
		log.info("appToken={},myUid={}", appToken, myUid);
	}

	public final static void pushSystem1(String content) {
		WxPushUtil.pushMsg(Message.CONTENT_TYPE_TEXT, content, true, null);
	}

	public final static void pushSystem2Html(String content) {
		WxPushUtil.pushMsg(Message.CONTENT_TYPE_HTML, content, true, null);
	}

	private final static void pushMsg(int contentType, String content, boolean isMyId, Set<String> uids) {
		try {
			Message message = new Message();
			message.setAppToken(appToken);
			message.setContentType(contentType);
			message.setContent(content + env + DateUtil.getTodayYYYYMMDDHHMMSS());
			if (isMyId) {
				message.setUid(myUid);
			} else {
				message.setUids(uids);
			}
			message.setUrl(null);
			Result<List<MessageResult>> result = WxPusher.send(message);
			List<MessageResult> lresult = result.getData();
			// log.info("result:{}", lresult);
			MessageResult mr = lresult.get(0);
			log.info("微信推送内容:{},状态:{}", content, mr.getStatus());
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, "微信推送内容异常", content, "");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Set<String> uids = new HashSet<String>();
		// uids.add("");
		// uids.add("");
		Message message = new Message();
		message.setAppToken("");
		message.setContentType(Message.CONTENT_TYPE_TEXT);
		message.setContent("不加限制的自由是很可怕的，因为很容易让任何人滑向深渊。-- " + DateUtil.getTodayYYYYMMDDHHMMSS());
		message.setUids(uids);
		message.setUrl(null);
		System.err.println(WxPusher.send(message).getData().get(0).getStatus());
	}
}
