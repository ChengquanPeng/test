package com.stable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;

@Target(value = ElementType.METHOD) // 声明该注解的运行目标: 方法
@Retention(value = RetentionPolicy.RUNTIME) // 该注解的生命周期: 运行时
public @interface RunLogAnn { // 通过@interface表示注解类型
	
	RunLogBizTypeEnum btype();

	RunCycleEnum runCycle() default RunCycleEnum.MANUAL;
}
