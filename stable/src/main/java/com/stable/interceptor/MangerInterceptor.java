package com.stable.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.alibaba.fastjson.JSON;
import com.stable.constant.Constant;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;

@Component
public class MangerInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
//		if (Constant.NEED_LOGIN) {
//		System.out.println(request.getRequestURI());
		HttpSession session = request.getSession();
		// 这里的User是登陆时放入session的
		Object user = session.getAttribute(Constant.SESSION_USER);
		// 如果session中没有user，表示没登陆
		if (user == null) {
			// 这个方法返回false表示忽略当前请求，如果一个用户调用了需要登陆才能使用的接口，如果他没有登陆这里会直接忽略掉
			// 当然你可以利用response给用户返回一些提示信息，告诉他没登陆
			JsonResult r = new JsonResult();
			r.setStatus("NO");
			r.setResult("未登录，非法访问！");
			response.setContentType("application/json;charset=utf-8");
			response.getWriter().write(JSON.toJSONString(r));
			return false;
		} else {
			try {
				UserInfo ui = (UserInfo) user;
				if (ui.getId() == Constant.MY_ID) {// 本人ID
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

}
