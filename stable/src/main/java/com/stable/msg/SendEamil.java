package com.stable.msg;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class SendEamil {

	@Autowired
	private JavaMailSender javaMailSender;

	public void text() throws Exception {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setText("内容abc");
		message.setSubject("主题123");
		message.setBcc("1832508436@qq.com");
		message.setFrom("348000443@qq.com");
		javaMailSender.send(message);
	}

	@PostConstruct
	public void html() throws Exception {
		MimeMessage mailMessage = javaMailSender.createMimeMessage();
		// 需要借助Helper类
		MimeMessageHelper helper = new MimeMessageHelper(mailMessage);
		String context = "<b>尊敬的用户：</b><br>        您好，管理员已为你申请了新的账号，"
				+ "请您尽快通过<a href=\"http://www.liwz.top/\">链接</a>登录系统。"
				+ "<br>修改密码并完善你的个人信息。<br><br><br><b>员工管理系统<br>Li，Wan Zhi</b>";
		try {

			helper.setSubject("主题123");
			helper.setBcc(new String[] { "1832508436@qq.com", "348000443@qq.com" });
			helper.setFrom("348000443@qq.com");
			helper.setSentDate(new Date());// 发送时间
			helper.setText(context, true);
			// 第一个参数要发送的内容，第二个参数是不是Html格式。
			javaMailSender.send(mailMessage);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
}