package com.stable.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DbOperatingAop {

	/**
	 * 指定切点 匹配 com.example.demo.controller包及其子包下的所有类的所有方法
	 */
	@Pointcut("execution(public * com.stable.db.dao.*.*(..))")
	public void printsqlargs() {
	}

	/**
	 * 后置异常通知
	 * 
	 * @param jp
	 */
	@AfterThrowing(value="printsqlargs()",throwing = "ex")
	public void printsqlexp(JoinPoint jp,Exception ex) {
		System.out.println("参数列表是:{" + Arrays.asList(jp.getArgs()) + "}");
	}

}
