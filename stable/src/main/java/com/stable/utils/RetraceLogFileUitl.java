package com.stable.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RetraceLogFileUitl {

	private final File file;
	private FileOutputStream fos;
	private FileChannel channel;
	private final String NEXT_LINE = "\n";

	public RetraceLogFileUitl(String filename) {
		file = new File("/my/free/retrace/" + filename);
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

	public void writeLine(String line) {
		try {
			// System.err.println(s.toString());
			line += NEXT_LINE;
			ByteBuffer buf = ByteBuffer.wrap(line.getBytes());
			buf.put(line.toString().getBytes());
			buf.flip();
			channel.write(buf);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
