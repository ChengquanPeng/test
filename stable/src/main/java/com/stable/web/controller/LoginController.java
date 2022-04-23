package com.stable.web.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.Constant;
import com.stable.service.UserService;
import com.stable.utils.DateUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
public class LoginController {
	@Autowired
	private UserService userService;

	@RequestMapping(value = "/sendkey", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> sendkey(String phone) {
		JsonResult r = new JsonResult();
		UserInfo ui = userService.getListById(Long.valueOf(phone));
		if (ui == null || ui.getS1() < DateUtil.getTodayIntYYYYMMDD()) {
			r.setStatus(JsonResult.FAIL);
			r.setResult("请联系管理员进行服务续约,抖音id=xxx");
		} else {
			if (StringUtils.isNotBlank(ui.getWxpush())) {
				WxPushUtil.pushSystem1(ui.getWxpush(), "xxxxTODO-redis-expir");
				r.setResult("动态码已发送到微信推送id=" + ui.getWxpush());
				r.setStatus(JsonResult.OK);
			} else {
				r.setResult("请联系管理员设置微信推送id");
				r.setStatus(JsonResult.FAIL);
			}
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/mylogin", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> mylogin(HttpServletRequest req) {
		JsonResult r = new JsonResult();
		if ("3n10b".equals(req.getParameter("key"))) {
			UserInfo ui = new UserInfo();
			ui.setType(1);
			req.getSession().setAttribute(Constant.SESSION_USER, ui);
			String logmsg = "mylogin 成功登录，时间：" + (new Date());
			r.setStatus(JsonResult.OK);
			r.setResult(logmsg);
			log.info(logmsg);
		} else {
			r.setStatus(JsonResult.FAIL);
			r.setResult("登录失败");
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> login(String phone, String code, HttpServletRequest req,
			HttpServletResponse response) {
		JsonResult r = new JsonResult();
		if (code.equals("xxxxTODO-redis-expir")) {
			UserInfo ui = new UserInfo();
			ui.setType(2);
			req.getSession().setAttribute(Constant.SESSION_USER, ui);
			r.setStatus(JsonResult.OK);
		} else {
			r.setStatus(JsonResult.FAIL);
			r.setResult("动态码错误");
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> logout(HttpServletRequest req) {
		JsonResult r = new JsonResult();
		req.getSession().removeAttribute(Constant.SESSION_USER);
		String logmsg = "mylogin 退出，时间：" + (new Date());
		r.setStatus(JsonResult.OK);
		r.setResult(logmsg);
		log.info(logmsg);
		return ResponseEntity.ok(r);
	}

	public static void main(String[] args) {
		System.err.println(Integer.valueOf("17603020611"));
	}
}
