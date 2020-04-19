package com.stable.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

	public static Properties getProperties() {
		try {
			Properties pro = new Properties();
			InputStream in;
			if (OSystemUtil.isLinux()) {
				in = new FileInputStream("/my/free/application.properties");
			} else {
				in = PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties");
			}
			pro.load(in);
			in.close();
			return pro;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
