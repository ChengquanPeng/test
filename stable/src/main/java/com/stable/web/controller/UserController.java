package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.UserService;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RestController("/manager")
public class UserController {

	@Autowired
	private UserService userService;

	/**
	 * 根据ID查询用户
	 */
	@RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> getUserById(@PathVariable(value = "id") Integer id) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(userService.getListById(id));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 查询用户列表
	 */
	@RequestMapping(value = "/user/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> getUserList(UserInfo user, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(userService.getList(user, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 添加用户
	 */
	@RequestMapping(value = "/user/add", method = RequestMethod.POST)
	public ResponseEntity<JsonResult> add(@RequestBody UserInfo user) {
		JsonResult r = new JsonResult();
		try {
			userService.add(user);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 用户充值
	 */
	@RequestMapping(value = "/user/amt")
	public ResponseEntity<JsonResult> update(@PathVariable("id") int id, double amt, int stype, int month) {
		JsonResult r = new JsonResult();
		try {
			userService.update(id, amt, stype, month);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 用户充值
	 */
	@RequestMapping(value = "/user/manul/updateamt")
	public ResponseEntity<JsonResult> update(@PathVariable("id") int id, int stype, int days) {
		JsonResult r = new JsonResult();
		try {
			userService.manulUpdate(id, stype, days);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
