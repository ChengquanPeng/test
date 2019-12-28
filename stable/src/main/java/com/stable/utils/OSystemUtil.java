package com.stable.utils;

public class OSystemUtil {
	public static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().contains("linux");
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}
}
