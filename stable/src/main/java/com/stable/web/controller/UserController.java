package com.stable.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.UserService;
import com.stable.service.model.WebModelService;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RestController
@RequestMapping("/manager")
public class UserController {

	@Autowired
	private UserService userService;
	@Autowired
	private WebModelService modelWebService;

	/**
	 * 根据ID查询用户
	 */
	@RequestMapping(value = "/user/{id}")
	public ResponseEntity<JsonResult> getUserById(@PathVariable(value = "id") long id) {
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
				user.setId(Long.valueOf(id));
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
			user.setId(Long.valueOf(id));
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
			user.setId(Long.valueOf(id));
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
	public ResponseEntity<JsonResult> amt(long id, double amt, int stype, int month) {
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
	 * 更新
	 */
	@RequestMapping(value = "/user/manul/updateamt")
	public ResponseEntity<JsonResult> updateamt(long id, int stype, int days) {
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

	/**
	 * pvlist-私有列表
	 */
	@RequestMapping(value = "/my/pvlist")
	public ResponseEntity<JsonResult> pvlist() {
		JsonResult r = new JsonResult();
		try {
			r.setResult(this.modelWebService.pvlist);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * pvlist-私有列表
	 */
	@RequestMapping(value = "/my/addpvlist")
	public ResponseEntity<JsonResult> addpvlist(String pvlist) {
		JsonResult r = new JsonResult();
		try {
			modelWebService.addPvList(pvlist == null ? "" : pvlist.trim());
			r.setResult(this.modelWebService.pvlist);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * pvlist-私有列表
	 */
	@RequestMapping(value = "/sendmsg")
	public ResponseEntity<JsonResult> sendmsg(int type, String msg) {
		JsonResult r = new JsonResult();
		try {
			
			r.setResult(this.modelWebService.pvlist);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
