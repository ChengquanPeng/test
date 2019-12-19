package com.stable.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
//@Component
public class SystemJobRunAop {

	/**
	 * 指定切点 匹配 com.example.demo.controller包及其子包下的所有类的所有方法
	 */
	@Pointcut("execution(public * com.stable.service.*.*(..))")
	public void printsqlargs() {
	}

	@AfterThrowing(value="printsqlargs()")
	public void printsqlexp(JoinPoint jp,Exception ex) {
		System.out.println("参数列表是:{" + Arrays.asList(jp.getArgs()) + "}");
	}

}
