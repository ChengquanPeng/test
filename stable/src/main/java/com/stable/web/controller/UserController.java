package com.stable.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.UserService;
import com.stable.vo.UserVo;
import com.stable.vo.http.JsonResult;

@RestController
public class UserController {

	@Autowired
	private UserService userService;

	/**
	 * 根据ID查询用户
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "user/{id}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> getUserById(@PathVariable(value = "id") Integer id) {
		JsonResult r = new JsonResult();
		try {

			UserVo user = userService.getUserById(id);
			r.setResult(user);
			r.setStatus("ok");
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus("error");
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 查询用户列表
	 * 
	 * @return
	 */
	@RequestMapping(value = "users", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> getUserList() {
		JsonResult r = new JsonResult();
		try {
			List<UserVo> users = userService.getUserList();
			r.setResult(users);
			r.setStatus("ok");
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus("error");
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 添加用户
	 * 
	 * @param user
	 * @return
	 * 
	 * @requestBody注解常用来处理content-type不是默认的application/x-www-form-urlcoded编码的内容，
	 * 
	 *                                                                           比如说：application/json或者是application/xml等。一般情况下来说常用其来处理application/json类型。
	 * 
	 *                                                                           就是从json中提取数据
	 *                                                                           参见：https://www.cnblogs.com/qiankun-site/p/5774300.html
	 */
	@RequestMapping(value = "user", method = RequestMethod.POST)
	public ResponseEntity<JsonResult> add(@RequestBody UserVo user) {
		JsonResult r = new JsonResult();
		try {
			int orderId = userService.add(user);
			if (orderId < 0) {
				r.setResult(orderId);
				r.setStatus("fail");
			} else {
				r.setResult(orderId);
				r.setStatus("ok");
			}
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus("error");

			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据id删除用户
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "user/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<JsonResult> delete(@PathVariable(value = "id") Integer id) {
		JsonResult r = new JsonResult();
		try {
			int ret = userService.delete(id);
			if (ret < 0) {
				r.setResult(ret);
				r.setStatus("fail");
			} else {
				r.setResult(ret);
				r.setStatus("ok");
			}
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus("error");

			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据id修改用户信息
	 * 
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "user/{id}", method = RequestMethod.PUT)
	public ResponseEntity<JsonResult> update(@PathVariable("id") Integer id, @RequestBody UserVo user) {
		JsonResult r = new JsonResult();
		try {
			int ret = userService.update(id, user);
			if (ret < 0) {
				r.setResult(ret);
				r.setStatus("fail");
			} else {
				r.setResult(ret);
				r.setStatus("ok");
			}
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus("error");

			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
