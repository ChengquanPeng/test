package com.stable.service.model.prd.msg;

import java.util.Date;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.utils.DateUtil;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SendEamilService implements InitializingBean {

	@Autowired
	private JavaMailSender javaMailSender;

	@Value("${mail.myid}")
	public String myId;
	@Value("${spring.mail.username}")
	private String fromId;

	private String fromName;

	public boolean pushSystemT1(String title, String content, String... toId) {
		try {
			for (String u : toId) {
				log.info(u);
			}
			log.info(title);
			log.info(content);
			SimpleMailMessage message = new SimpleMailMessage();
			message.setText(content + " " + WxPushUtil.env + DateUtil.getTodayYYYYMMDDHHMMSS());
			message.setSubject(title);
			message.setBcc(toId);
			message.setFrom(fromName);
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
			for (String u : toId) {
				log.info(u);
			}
			log.info(title);
			log.info(content);
			helper.setSubject(title);
			helper.setBcc(toId);
			helper.setFrom(fromName);
			helper.setSentDate(new Date());// 发送时间
			helper.setText(content + Constant.HTML_LINE + WxPushUtil.env + DateUtil.getTodayYYYYMMDDHHMMSS(), true);// 第一个参数要发送的内容，第二个参数是不是Html格式。
			javaMailSender.send(mailMessage);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		fromName = "底部炒家" + "<" + fromId + ">";
	}

}