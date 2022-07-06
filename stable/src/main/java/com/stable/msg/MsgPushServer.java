package com.stable.msg;

import java.util.List;

import com.stable.config.SpringConfig;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.SpringUtil;
import com.stable.utils.ThreadsUtil;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import com.zjiecode.wxpusher.client.bean.MessageResult;
import com.zjiecode.wxpusher.client.bean.Page;
import com.zjiecode.wxpusher.client.bean.Result;
import com.zjiecode.wxpusher.client.bean.WxUser;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class MsgPushServer {

	private static String appToken;
	public static String myUid;
	private static String env;
	static {
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		appToken = efc.getAppToken().trim();
		myUid = efc.getMyUid().trim();
		env = (SpringConfig.isWindows ? " windows" : "") + " ";
		log.info("appToken={},myUid={}", appToken, myUid);
	}

	public final static void pushSystem1(String content) {
		MsgPushServer.pushMsg(Message.CONTENT_TYPE_TEXT, content, myUid);
	}

	public final static boolean pushSystem1(String uid, String content) {
		return MsgPushServer.pushMsg(Message.CONTENT_TYPE_TEXT, content, uid);
	}

	public final static boolean pushSystem2Html(String uid, String content) {
		return MsgPushServer.pushMsg(Message.CONTENT_TYPE_HTML, content, uid);
	}

	public final static void pushSystem2Html(String content) {
		MsgPushServer.pushMsg(Message.CONTENT_TYPE_HTML, content, myUid);
	}

	private final static boolean pushMsg(int contentType, String content, String singleId) {
		return pushMsgWarp(contentType, content, singleId);
	}

	private final static boolean pushMsgWarp(int contentType, String content, String singleId) {
		if (pushMsg(contentType, content, singleId, false)) {
			return true;
		}
		int i = 1;
		while (i <= 3) {
			ThreadsUtil.sleepRandomSecBetween1And5();
			if (pushMsg(contentType, content, singleId, (i == 3))) {
				return true;
			}
			i++;
		}
		return false;
	}

	private final static boolean pushMsg(int contentType, String content, String singleId, boolean showErrorLog) {
		try {
			Message message = new Message();
			message.setAppToken(appToken);
			message.setContentType(contentType);
			message.setContent(content + env + DateUtil.getTodayYYYYMMDDHHMMSS());
			message.setUid(singleId);
//			if (StringUtils.isNotBlank(singleId)) {
//				
//			} else {
//				message.setUids(uids);
//			}
			message.setUrl(null);
			Result<List<MessageResult>> result = WxPusher.send(message);
			List<MessageResult> lresult = result.getData();
			// log.info("result:{}", lresult);
			MessageResult mr = lresult.get(0);
			log.info("微信推送内容:{},状态:{}", content, mr.getStatus());
			return true;
		} catch (Exception e) {
			if (showErrorLog) {
				ErrorLogFileUitl.writeError(e, "微信推送内容异常", singleId, content);
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void main(String[] args) {
//		Set<String> uids = new HashSet<String>();
//		// uids.add("");
//		// uids.add("");
//		Message message = new Message();
//		message.setAppToken("");
//		message.setContentType(Message.CONTENT_TYPE_TEXT);
//		message.setContent("不加限制的自由是很可怕的，因为很容易让任何人滑向深渊。-- " + DateUtil.getTodayYYYYMMDDHHMMSS());
//		message.setUids(uids);
//		message.setUrl(null);
//		System.err.println(WxPusher.send(message).getData().get(0).getStatus());

		Result<Page<WxUser>> users = WxPusher.queryWxUser("AT_X5jTnRko4rhQ4dmoS7ntwBN79BxATk3r", 1, 100);
		List<WxUser> list = users.getData().getRecords();
		for (int i = 0; i < list.size(); i++) {
			WxUser wu = list.get(i);
			System.err.println(wu.getUid() + "," + wu.getNickName() + ",");
		}
	}
}
