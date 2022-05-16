package com.stable.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileWriteUitl {

	public static final String LINE_FILE = "\n";
	public static final String LINE_HTML = "</br>";
	private final File file;
	private final FileOutputStream fos;
	private final FileChannel channel;

	public FileWriteUitl(String filepath, boolean isNewFile) {
		try {
			log.info("File Path:{}", filepath);
			file = new File(filepath);
			if (isNewFile && file.exists()) {
				file.delete();
			}
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			fos = new FileOutputStream(file, true);
			channel = fos.getChannel();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeLine(String s) {
		try {
			s += LINE_FILE;
			ByteBuffer buf = ByteBuffer.wrap(s.toString().getBytes());
			buf.put(s.toString().getBytes());
			buf.flip();

			channel.write(buf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
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
