package com.stable.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.stable.config.ErrorLogFileConfig;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ErrorLogFileUitl {

	private static final File file;
	private static FileOutputStream fos;
	private static FileChannel channel;
	private static final String LINE = "\n";
	static {
		ErrorLogFileConfig efc = SpringUtil.getBean(ErrorLogFileConfig.class);
		String filepath = efc.getFilepath();
		log.info("ERROR File Path:{}", filepath);
		file = new File(filepath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			fos = new FileOutputStream(file, true);
			channel = fos.getChannel();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
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
		}));
	}

	public static void writeError(Exception e, String p1, String p2, String p3) {
		try {
			StringBuffer s = new StringBuffer();
			s.append("=========================:");
			s.append(DateUtil.getTodayYYYYMMDDHHMMSS());
			s.append(LINE);
			s.append(p1 + "|" + p2 + "|" + p3 + "|" + e.getMessage());
			s.append(LINE);
			for (StackTraceElement se : e.getStackTrace()) {
				s.append(se.toString());
				s.append(LINE);
			}
			// System.err.println(s.toString());
			ByteBuffer buf = ByteBuffer.wrap(s.toString().getBytes());
			buf.put(s.toString().getBytes());
			buf.flip();
			channel.write(buf);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
