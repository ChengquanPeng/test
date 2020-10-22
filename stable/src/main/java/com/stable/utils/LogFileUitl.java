package com.stable.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class LogFileUitl {

	public static void writeFileWithGBK(String filepath, String content) {
		File file = null;
		OutputStreamWriter fos = null;
		try {
			log.info("Log File Path:{}", filepath);
			file = new File(filepath);
			if (!file.exists()) {
				try {
					if (file.getParentFile() != null && !file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
					}
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				fos = new OutputStreamWriter(new FileOutputStream(file), "GBK");
				fos.write(content);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void writeLog(String filepath, String content) {
		File file = null;
		FileOutputStream fos = null;
		FileChannel channel = null;
		try {
			log.info("Log File Path:{}", filepath);
			file = new File(filepath);
			if (!file.exists()) {
				try {
					if (file.getParentFile() != null && !file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
					}
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				fos = new FileOutputStream(file, false);
				channel = fos.getChannel();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// System.err.println(s.toString());
			ByteBuffer buf = ByteBuffer.wrap(content.getBytes());
			buf.put(content.getBytes());
			buf.flip();
			channel.write(buf);
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
