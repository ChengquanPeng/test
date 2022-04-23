package com.stable.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.UserService;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RestController
@RequestMapping("/manager")
public class UserController {

	@Autowired
	private UserService userService;

	/**
	 * 根据ID查询用户
	 */
	@RequestMapping(value = "/user/{id}")
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
	@RequestMapping(value = "/user/list")
	public ResponseEntity<JsonResult> getUserList(String id, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			UserInfo user = new UserInfo();
			if (StringUtils.isNotBlank(id)) {
				user.setId(Integer.valueOf(id));
			}
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
	@RequestMapping(value = "/user/add")
	public ResponseEntity<JsonResult> add(String id, String remark, String name, String wxpush) {
		JsonResult r = new JsonResult();
		try {
			UserInfo user = new UserInfo();
			user.setId(Integer.valueOf(id));
			user.setRemark(remark.trim());
			user.setWxpush(wxpush.trim());
			user.setName(name.trim());
			user.setType(2);
			userService.add(user);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 修改信息
	 */
	@RequestMapping(value = "/user/updateinfo")
	public ResponseEntity<JsonResult> updateinfo(String id, String remark, String name, String wxpush) {
		JsonResult r = new JsonResult();
		try {
			UserInfo user = new UserInfo();
			user.setId(Integer.valueOf(id));
			user.setRemark(remark.trim());
			user.setWxpush(wxpush.trim());
			user.setName(name.trim());
			userService.updateinfo(user);
			r.setStatus(JsonResult.OK);
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
	public ResponseEntity<JsonResult> amt(int id, double amt, int stype, int month) {
		JsonResult r = new JsonResult();
		try {
			userService.update(id, amt, stype, month);
			r.setStatus(JsonResult.OK);
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
	public ResponseEntity<JsonResult> updateamt(int id, int stype, int days) {
		JsonResult r = new JsonResult();
		try {
			userService.manulUpdate(id, stype, days);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
