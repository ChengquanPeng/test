package com.stable.msg;

import java.util.Date;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.utils.DateUtil;

@Service
public class SendEamilService {

	@Autowired
	private JavaMailSender javaMailSender;

	@Value("${mail.myid}")
	public String myId;
	@Value("${spring.mail.username}")
	private String fromId;

	public boolean pushSystemT1(String title, String content, String... toId) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setText(content + " " + WxPushUtil.env + DateUtil.getTodayYYYYMMDDHHMMSS());
			message.setSubject(title);
			message.setBcc(toId);
			message.setFrom(fromId);
			javaMailSender.send(message);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean pushSystemHtmlT2(String title, String content, String... toId) {
		MimeMessage mailMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mailMessage);// 需要借助Helper类
		try {
			helper.setSubject(title);
			helper.setBcc(toId);
			helper.setFrom(fromId);
			helper.setSentDate(new Date());// 发送时间
			helper.setText(content + Constant.HTML_LINE + WxPushUtil.env + DateUtil.getTodayYYYYMMDDHHMMSS(), true);// 第一个参数要发送的内容，第二个参数是不是Html格式。
			javaMailSender.send(mailMessage);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}