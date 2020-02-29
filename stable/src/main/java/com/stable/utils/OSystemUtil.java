package com.stable.utils;

import java.io.IOException;

public class OSystemUtil {
	public static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().contains("linux");
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	public static void restart() {
		if (isLinux()) {
			try {
				Runtime.getRuntime().exec("reboot");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
