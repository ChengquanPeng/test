package com.stable.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.stable.config.SpringConfig;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class NoTickDataLogFileUitl {

	private static File file;
	private static FileOutputStream fos;
	private static FileChannel channel;
	private static String LINE = "\n";
	private static String curr_date = "";

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				close();
			}
		}));
	}

	private final static void create(String date) {
		close();// closed first

		// then create
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		String filepath = efc.getNotickdata() + date + ".log";
		log.info("Log File Path:{}", filepath);
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

	}

	private final static FileChannel getAndCreate(String date) {
		if (date.equals(curr_date)) {
			return channel;
		}
		create(date);
		return channel;
	}

	private final static void close() {
		try {
			channel.close();
		} catch (Exception e) {
		}
		try {
			fos.close();
		} catch (Exception e) {
		}
	}

	public final static void writeLog(String date, String content) {
		try {
			StringBuffer s = new StringBuffer();
			s.append(content);
			s.append(LINE);
			ByteBuffer buf = ByteBuffer.wrap(s.toString().getBytes());
			buf.put(s.toString().getBytes());
			buf.flip();
			// channel.write(buf);
			getAndCreate(date).write(buf);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
