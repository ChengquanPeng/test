package com.stable.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class DowloadTsByFfmpeg {

	private static String FFMPEG = "E:/server/ffmpeg/bin/ffmpeg";

	public static void main(String[] args) {
		StringBuffer files = new StringBuffer();
		String path = "E:/server/ffmpeg/bin/tmp/";
		String fileName = path + "files.txt"; // 文件名
		String mregFileName = path + "my.mp4"; // 合并文件名
		String cmd = "";
		for (int i = 1; i <= 470; i++) {

			cmd = FFMPEG + " -i https://cdn-t.asujp.com:59666/data7/9AC621712A214DC4/5F245B8FCE4263C2/ts2/out" + i
					+ ".ts -c copy " + path + i + ".ts";
			runCmd(cmd);
			files.append("file '" + path + i + ".ts'").append("\n");
		}
		createFile(fileName, files.toString());
		mreg(fileName, mregFileName);
	}

	public static void mreg(String fileName, String mregFileName) {
		String cmd = FFMPEG + " -f concat -safe 0  -i " + fileName + " -c:v copy -c:a copy -bsf:a aac_adtstoasc "
				+ mregFileName;
		runCmd(cmd);
	}

	public static void createFile(String fileName, String data) {
		try {
			File file = new File(fileName); // 创建File对象
			if (file.exists()) {
				file.delete();
			}
			FileOutputStream fos = new FileOutputStream(file); // 创建FileOutputStream对象

			fos.write(data.getBytes()); // 写入数据

			fos.close(); // 关闭输出流
			System.out.println("文件创建并写入成功！");
		} catch (Exception e) {
			System.out.println(fileName + "文件创建或写入失败：" + e.getMessage());
		}
	}

	public static void runCmd(String cmd) {
		// 使用Runtime.getRuntime().exec()来调用外部程序
		try {
			// 替换为你想要调用的外部程序和参数
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}

			process.waitFor(); // 等待外部程序执行完成
			if (process.exitValue() == 0) {
				System.out.println("完成：" + cmd);
			} else {
				System.err.println("异常：" + cmd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
