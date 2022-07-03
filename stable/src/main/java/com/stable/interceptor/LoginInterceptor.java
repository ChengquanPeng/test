package com.stable.interceptor;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.stable.constant.Constant;

@Component
public class LoginInterceptor implements HandlerInterceptor {

//	@Autowired
//	private LoginController loginController;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
//		if (Constant.NEED_LOGIN) {
		HttpSession session = request.getSession();
		// 这里的User是登陆时放入session的
		Object user = session.getAttribute(Constant.SESSION_USER);

//		if (user == null) {
//			loginController.mylogin(request);
//			user = session.getAttribute(Constant.SESSION_USER);
//		}

		// 如果session中没有user，表示没登陆
		if (user == null) {
			// System.err.println(request.getRequestURL());
			// 这个方法返回false表示忽略当前请求，如果一个用户调用了需要登陆才能使用的接口，如果他没有登陆这里会直接忽略掉
			// 当然你可以利用response给用户返回一些提示信息，告诉他没登陆
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html;charset=utf-8");
			try {
				PrintWriter w = response.getWriter();
				w.write("<script>window.location='/web/login.html?showtip=1';</script>");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		} else {
			return true;
		}
	}

}
