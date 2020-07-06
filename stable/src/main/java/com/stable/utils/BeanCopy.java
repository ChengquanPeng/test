package com.stable.utils;

import org.springframework.cglib.beans.BeanCopier;

public class BeanCopy {

	public static void copy(Object source, Object target) {
		BeanCopier beanc = BeanCopier.create(source.getClass(), target.getClass(), false);
		beanc.copy(source, target, null);
	}
}
