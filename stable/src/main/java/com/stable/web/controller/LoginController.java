package com.stable.web.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.Constant;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
public class LoginController {

	@RequestMapping(value = "/mylogin", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> mylogin(HttpServletRequest req) {
		JsonResult r = new JsonResult();
		req.getSession().setAttribute(Constant.SESSION_USER, Constant.SESSION_USER);
		String logmsg = "mylogin 成功登录，时间：" + (new Date());
		r.setStatus("ok");
		r.setResult(logmsg);
		log.info(logmsg);
		return ResponseEntity.ok(r);
	}
	
	@RequestMapping(value = "/mylogout", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> logout(HttpServletRequest req) {
		JsonResult r = new JsonResult();
		req.getSession().removeAttribute(Constant.SESSION_USER);
		String logmsg = "mylogin 退出，时间：" + (new Date());
		r.setStatus("ok");
		r.setResult(logmsg);
		log.info(logmsg);
		return ResponseEntity.ok(r);
	}
}
